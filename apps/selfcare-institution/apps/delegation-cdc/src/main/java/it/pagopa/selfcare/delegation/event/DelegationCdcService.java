package it.pagopa.selfcare.delegation.event;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.microsoft.applicationinsights.TelemetryClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.client.EventHubRestClient;
import it.pagopa.selfcare.delegation.event.config.ConfigUtilsBean;
import it.pagopa.selfcare.delegation.event.constant.DelegationType;
import it.pagopa.selfcare.delegation.event.constant.RelationshipState;
import it.pagopa.selfcare.delegation.event.entity.DelegationsEntity;
import it.pagopa.selfcare.delegation.event.entity.Institution;
import it.pagopa.selfcare.delegation.event.entity.OnboardingEntity;
import it.pagopa.selfcare.delegation.event.mapper.DelegationMapper;
import it.pagopa.selfcare.delegation.event.repository.DelegationRepository;
import it.pagopa.selfcare.delegation.event.repository.InstitutionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static it.pagopa.selfcare.delegation.event.constant.CdcStartAtConstant.*;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class DelegationCdcService {
    private static final String COLLECTION_NAME = "Delegations";
    private static final String DELEGATION_INSERT_FAILURE = "DelegationInsert_failure";
    private static final String DELEGATION_INSERT_SUCCESS = "DelegationInsert_success";
    private static final String DELEGATION_EVENT_FAILURE = "DelegationEvent_failure";
    private static final String DELEGATION_EVENT_SUCCESS = "DelegationEvent_success";
    private static final String ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE = "Error during subscribe collection, exception: {} , message: {}";
    private static final String EVENT_INSTITUTION_CDC_NAME = "DELEGATION_CDC";
    private static final String OPERATION_NAME = "DELEGATION-CDC-DelegationInsert";
    private final TelemetryClient telemetryClient;
    private final TableClient tableClient;
    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;
    private final ConfigUtilsBean configUtilsBean;
    private final InstitutionRepository institutionRepository;
    private final DelegationRepository delegationRepository;
    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;
    private final List<String> availableProducts;
    private final boolean sendEventsEnabled;

    @Inject
    private DelegationMapper delegationMapper;

    @RestClient
    @Inject
    private EventHubRestClient eventHubRestClient;

    public DelegationCdcService(ReactiveMongoClient mongoClient,
                                @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                                TelemetryClient telemetryClient,
                                TableClient tableClient,
                                ConfigUtilsBean configUtilsBean,
                                InstitutionRepository institutionRepository,
                                DelegationRepository delegationRepository,
                                @ConfigProperty(name = "delegation-cdc.retry.min-backoff") Integer retryMinBackOff,
                                @ConfigProperty(name = "delegation-cdc.retry.max-backoff") Integer retryMaxBackOff,
                                @ConfigProperty(name = "delegation-cdc.retry") Integer maxRetry,
                                @ConfigProperty(name = "delegation-cdc.products.available") List<String> availableProducts,
                                @ConfigProperty(name = "delegation-cdc.send-events.watch.enabled") Boolean sendEventsEnabled) {
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.telemetryClient = telemetryClient;
        this.tableClient = tableClient;
        this.configUtilsBean = configUtilsBean;
        this.institutionRepository = institutionRepository;
        this.delegationRepository = delegationRepository;
        this.retryMinBackOff = retryMinBackOff;
        this.retryMaxBackOff = retryMaxBackOff;
        this.maxRetry = maxRetry;
        this.availableProducts = availableProducts;
        this.sendEventsEnabled = sendEventsEnabled;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream();
    }

    private void initOrderStream() {
        log.info("Starting initOrderStream ... ");

        //Retrieve last resumeToken for watching collection at specific operation
        String resumeToken = null;

        if (!configUtilsBean.getProfiles().contains("test")) {
            try {
                TableEntity cdcStartAtEntity = tableClient.getEntity(CDC_START_AT_PARTITION_KEY, CDC_START_AT_ROW_KEY);
                if (Objects.nonNull(cdcStartAtEntity))
                    resumeToken = (String) cdcStartAtEntity.getProperty(CDC_START_AT_PROPERTY);
            } catch (TableServiceException e) {
                log.warn("Table StarAt not found, it is starting from now ...");
            }
        }

        // Initialize watching collection
        ReactiveMongoCollection<DelegationsEntity> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
        if (Objects.nonNull(resumeToken))
            options = options.resumeAfter(BsonDocument.parse(resumeToken));

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<DelegationsEntity>> publisher = dataCollection.watch(pipeline, DelegationsEntity.class, options);

        publisher.subscribe().with(
                this::consumerDelegationRepositoryEvent,
                failure -> {
                    log.error(ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE, failure.toString(), failure.getMessage());
                    constructMapAndTrackEvent(failure.getClass().toString(), "FALSE", DELEGATION_INSERT_FAILURE);
                    Quarkus.asyncExit();
                });
        log.info("Completed initOrderStream ... ");
    }

    private ReactiveMongoCollection<DelegationsEntity> getCollection() {
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, DelegationsEntity.class);
    }

    public void consumerDelegationRepositoryEvent(ChangeStreamDocument<DelegationsEntity> document) {
        assert document.getDocumentKey() != null;

        DelegationsEntity insertedDelegation = document.getFullDocument();
        assert insertedDelegation != null;
        String delegationId = insertedDelegation.getId();
        log.info("Starting consumerDelegationsRepositoryEvent from Delegation document having id: {}", delegationId);

        Uni<Boolean> createAggregatesDelegationsUni = Uni.createFrom().item(true);
        Uni<Boolean> sendEventsUni = Uni.createFrom().item(true);

        if(DelegationType.PT.equals(insertedDelegation.getType()) && availableProducts.contains(insertedDelegation.getProductId())) {
            createAggregatesDelegationsUni = attemptToCreateAggregatesDelegations(insertedDelegation)
                    .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                    .onItem().invoke(result -> {
                        log.info("attemptToCreateAggregatesDelegations successfully performed for Delegation document having id: {}", delegationId);
                        constructMapAndTrackEvent(document.getDocumentKey().toJson(), "TRUE", DELEGATION_INSERT_SUCCESS);
                    })
                    .onItem().transformToUni(result -> Uni.createFrom().item(true))
                    .onFailure().invoke(failure -> {
                        log.error("Error during Delegation collection updating, from Delegation document having id: {} , message: {}", delegationId, failure.getMessage());
                        constructMapAndTrackEvent(document.getDocumentKey().toJson(), "FALSE", DELEGATION_INSERT_FAILURE);
                    })
                    .onFailure().recoverWithUni(failure -> Uni.createFrom().item(false));
        }

        if (sendEventsEnabled) {
            sendEventsUni = eventHubRestClient.sendMessage(delegationMapper.toDelegationNotificationToSend(insertedDelegation))
                    .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                    .onItem().invoke(result -> {
                        log.info("Notification for delegationId {} sent", delegationId);
                        constructMapAndTrackEvent(document.getDocumentKey().toJson(), "TRUE", DELEGATION_EVENT_SUCCESS);
                    })
                    .onItem().transformToUni(result -> Uni.createFrom().item(true))
                    .onFailure().invoke(failure -> {
                        log.error("Error while sending notification for delegationId {}: {}", delegationId, failure.getMessage());
                        constructMapAndTrackEvent(document.getDocumentKey().toJson(), "FALSE", DELEGATION_EVENT_FAILURE);
                    })
                    .onFailure().recoverWithUni(failure -> Uni.createFrom().item(false));
        }

        Uni.combine().all().unis(createAggregatesDelegationsUni, sendEventsUni)
                .with(Boolean::logicalAnd)
                .subscribe()
                .with(
                        result -> {
                            if (result) {
                                log.info("Updating last resume token after processing delegation document having id: {}", delegationId);
                                updateLastResumeToken(document.getResumeToken());
                            } else {
                                log.error("Error while processing delegation document having id {}", delegationId);
                            }
                        },
                        failure -> log.error("Error while processing delegation document having id {}: {}", delegationId, failure.getMessage())
                );
    }

    private Uni<Void> attemptToCreateAggregatesDelegations(DelegationsEntity insertedDelegation) {
        return institutionRepository.findInstitutionById(insertedDelegation.getFrom())
                .flatMap(institution -> {
                    if (institution == null) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Institution not found"));
                    }
                    return isAggregator(institution, insertedDelegation.getProductId())
                            .flatMap(isAggregator -> {
                                if (!Boolean.TRUE.equals(isAggregator)) {
                                    return Uni.createFrom().nullItem();
                                }
                                return delegationRepository.getInstitutionsAlreadyPresent(insertedDelegation.getTo(), insertedDelegation.getProductId())
                                        .collect().asList()
                                        .flatMap(existingInstitutions ->
                                                delegationRepository.getDelegationsEA(insertedDelegation.getFrom(), insertedDelegation.getProductId())
                                                .filter(delegation -> !existingInstitutions.contains(delegation.getFrom())) //filter the delegations already presents
                                                .collect().asList()
                                                .flatMap(delegations -> {
                                                    Multi<DelegationsEntity> mappedDelegations = mapDelegations(Multi.createFrom().items(delegations.stream()), Uni.createFrom().item(insertedDelegation));
                                                    return delegationRepository.insertDelegations(mappedDelegations);
                                                }));
                            });
                });
    }


    private void updateLastResumeToken(BsonDocument resumeToken) {
        // Table CdCStartAt will be updated with the last resume token
        Map<String, Object> properties = new HashMap<>();
        properties.put(CDC_START_AT_PROPERTY, resumeToken.toJson());

        TableEntity tableEntity = new TableEntity(CDC_START_AT_PARTITION_KEY, CDC_START_AT_ROW_KEY)
                .setProperties(properties);
        tableClient.upsertEntity(tableEntity);

    }


    private void constructMapAndTrackEvent(String documentKey, String success, String... metrics) {
        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("documentKey", documentKey);
        propertiesMap.put("success", success);

        Map<String, Double> metricsMap = new HashMap<>();
        Arrays.stream(metrics).forEach(metricName -> metricsMap.put(metricName, 1D));
        telemetryClient.trackEvent(EVENT_INSTITUTION_CDC_NAME, propertiesMap, metricsMap);
    }


    public Uni<Boolean> isAggregator(Institution institution, String productId) {
        return Uni.createFrom().item(() ->
                institution.getOnboarding().stream()
                        .filter(onboardingEntity ->
                                productId.equals(onboardingEntity.getProductId()) &&
                                        RelationshipState.ACTIVE.equals(onboardingEntity.getStatus()))
                        .max(Comparator.comparing(onboardingEntity ->
                                OffsetDateTime.parse(onboardingEntity.getCreatedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                        .map(OnboardingEntity::getIsAggregator)
                        .orElse(false)
        );
    }


    //Mapping the list of all delegations from aggregates to PT
    public Multi<DelegationsEntity> mapDelegations(Multi<DelegationsEntity> delegations, Uni<DelegationsEntity> insertedDelegation) {
        return delegations
                .onItem().transformToMulti(delegationsEntity ->
                        Uni.combine().all().unis(
                                        Uni.createFrom().item(delegationsEntity),
                                        insertedDelegation
                                ).asTuple()
                                .onItem().transformToMulti(tuple ->
                                        Multi.createFrom().item(delegationMapper.toDelegationAggregatePT(tuple.getItem1(), tuple.getItem2()))
                                )
                )
                .merge();
    }

}
