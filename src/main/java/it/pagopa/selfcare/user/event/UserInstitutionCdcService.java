package it.pagopa.selfcare.user.event;

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
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.UserUtils;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.mapper.NotificationMapper;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static it.pagopa.selfcare.user.UserUtils.mapPropsForTrackEvent;
import static it.pagopa.selfcare.user.event.constant.CdcStartAtConstant.*;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.*;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_CDC_NAME;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class UserInstitutionCdcService {

    private static final String COLLECTION_NAME = "userInstitutions";
    private static final String OPERATION_NAME = "USER-CDC-UserInfoUpdate";
    public static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";

    private final TelemetryClient telemetryClient;

    private final TableClient tableClient;
    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;
    private final UserInstitutionRepository userInstitutionRepository;

    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;


    @RestClient
    @Inject
    UserApi userRegistryApi;
    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;

    private final NotificationMapper notificationMapper;


    public UserInstitutionCdcService(ReactiveMongoClient mongoClient,
                                     @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                                     @ConfigProperty(name = "user-cdc.retry.min-backoff") Integer retryMinBackOff,
                                     @ConfigProperty(name = "user-cdc.retry.max-backoff") Integer retryMaxBackOff,
                                     @ConfigProperty(name = "user-cdc.retry") Integer maxRetry,
                                     @ConfigProperty(name = "user-cdc.send-events.watch.enabled") Boolean sendEventsEnabled,
                                     UserInstitutionRepository userInstitutionRepository,
                                     TelemetryClient telemetryClient,
                                     TableClient tableClient, NotificationMapper notificationMapper) {
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.userInstitutionRepository = userInstitutionRepository;
        this.maxRetry = maxRetry;
        this.retryMaxBackOff = retryMaxBackOff;
        this.retryMinBackOff = retryMinBackOff;
        this.telemetryClient = telemetryClient;
        this.tableClient = tableClient;
        this.notificationMapper = notificationMapper;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream(sendEventsEnabled);
    }

    private void initOrderStream(Boolean sendEventsEnabled) {
        log.info("Starting initOrderStream ... ");

        //Retrieve last resumeToken for watching collection at specific operation
        String resumeToken = null;

        if(!ConfigUtils.getProfiles().contains("test")) {
            try {
                TableEntity cdcStartAtEntity = tableClient.getEntity(CDC_START_AT_PARTITION_KEY, CDC_START_AT_ROW_KEY);
                if (Objects.nonNull(cdcStartAtEntity))
                    resumeToken = (String) cdcStartAtEntity.getProperty(CDC_START_AT_PROPERTY);
            } catch (TableServiceException e) {
                log.warn("Table StarAt not found, it is starting from now ...");
            }
        }

        // Initialize watching collection
        ReactiveMongoCollection<UserInstitution> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
        if(Objects.nonNull(resumeToken))
            options = options.resumeAfter(BsonDocument.parse(resumeToken));

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = dataCollection.watch(pipeline, UserInstitution.class, options);
        publisher.subscribe().with(
                this::consumerUserInstitutionRepositoryEvent,
                failure -> {
                    log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
                    telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(failure.getClass().toString(), null,null),  Map.of(USER_INFO_UPDATE_FAILURE, 1D));
                    Quarkus.asyncExit();
                });

        if(sendEventsEnabled) {
            publisher.subscribe().with(
                    this::consumerToSendScUserEvent,
                    failure -> {
                        log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
                        telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(failure.getClass().toString(), null,null), Map.of(EVENTS_USER_INSTITUTION_FAILURE, 1D));
                        Quarkus.asyncExit();
                    });
        }

        log.info("Completed initOrderStream ... ");
    }

    private ReactiveMongoCollection<UserInstitution> getCollection() {
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, UserInstitution.class);
    }

    protected void consumerUserInstitutionRepositoryEvent(ChangeStreamDocument<UserInstitution> document) {

        assert document.getFullDocument() != null;
        assert document.getDocumentKey() != null;
        UserInstitution userInstitutionChanged = document.getFullDocument();
        String userInstitutionId = userInstitutionChanged.getId().toHexString();

        log.info("Starting consumerUserInstitutionRepositoryEvent ... ");

        userInstitutionRepository.updateUser(userInstitutionChanged)
                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofHours(retryMaxBackOff)).atMost(maxRetry)
                .subscribe().with(
                        result -> {
                            log.info("UserInfo collection successfully updated from UserInstitution document having id: {}", userInstitutionId);
                            updateLastResumeToken(document.getResumeToken());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(userInstitutionId, userInstitutionChanged.getUserId(), null), Map.of(USER_INFO_UPDATE_SUCCESS, 1D));
                        },
                        failure -> {
                            log.error("Error during UserInfo collection updating, from UserInstitution document having id: {} , message: {}", userInstitutionId, failure.getMessage());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(userInstitutionId, userInstitutionChanged.getUserId(), null), Map.of(USER_INFO_UPDATE_FAILURE, 1D));
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

    public void consumerToSendScUserEvent(ChangeStreamDocument<UserInstitution> document) {

        assert document.getFullDocument() != null;
        assert document.getDocumentKey() != null;
        UserInstitution userInstitutionChanged = document.getFullDocument();

        log.info("Starting consumerUserInstitutionRepositoryEvent ... ");

        userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitutionChanged.getUserId())
                .onFailure(this::checkIfIsRetryableException)
                    .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                .onItem().transformToUni(userResource -> Multi.createFrom().iterable(UserUtils.groupingProductAndReturnMinStateProduct(userInstitutionChanged.getProducts()))
                        .map(onboardedProduct -> notificationMapper.toUserNotificationToSend(userInstitutionChanged, onboardedProduct, userResource))
                        .onItem().transformToUniAndMerge(userNotificationToSend -> eventHubRestClient.sendMessage(userNotificationToSend)
                            .onItem().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(userInstitutionChanged.getId().toHexString(), userInstitutionChanged.getUserId(), userNotificationToSend.getProductId()),  Map.of(EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS, 1D)))
                            .onFailure().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(userInstitutionChanged.getId().toHexString(), userInstitutionChanged.getUserId(), userNotificationToSend.getProductId()),  Map.of(EVENTS_USER_INSTITUTION_PRODUCT_FAILURE, 1D))))
                        .toUni()
                )
                .subscribe().with(
                        result -> {
                            log.info("SendEvents successfully performed from UserInstitution document having id: {}", document.getDocumentKey().toJson());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(userInstitutionChanged.getId().toHexString(), userInstitutionChanged.getUserId(), null), Map.of(EVENTS_USER_INSTITUTION_SUCCESS, 1D));
                        },
                        failure -> {
                            log.error("Error during SendEvents from UserInstitution document having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(userInstitutionChanged.getId().toHexString(), userInstitutionChanged.getUserId(), null), Map.of(EVENTS_USER_INSTITUTION_FAILURE, 1D));
                        });
    }

    private boolean checkIfIsRetryableException(Throwable throwable) {
        return throwable instanceof TimeoutException ||
                (throwable instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == 429);
    }
}
