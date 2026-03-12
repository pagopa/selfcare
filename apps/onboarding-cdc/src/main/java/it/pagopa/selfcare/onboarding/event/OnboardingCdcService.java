package it.pagopa.selfcare.onboarding.event;

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
import it.pagopa.selfcare.onboarding.event.constant.CdcStartAtConstant;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.COMPLETED;
import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.DELETED;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class OnboardingCdcService {
    private static final String COLLECTION_NAME = "onboardings";
    private static final String OPERATION_NAME = "ONBOARDING-CDC-OnboardingsUpdate";
    private static final String EVENT_NAME = "ONBOARDING-CDC";
    private static final String ONBOARDING_FAILURE_MECTRICS = "OnboardingsUpdate_failures";
    private static final String ONBOARDING_SUCCESS_MECTRICS = "OnboardingsUpdate_successes";
    private final TelemetryClient telemetryClient;
    private final TableClient tableClient;
    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;
    private final NotificationService notificationService;

    public OnboardingCdcService(ReactiveMongoClient mongoClient,
                                @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                                TelemetryClient telemetryClient,
                                TableClient tableClient,
                                NotificationService notificationService) {
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.telemetryClient = telemetryClient;
        this.tableClient = tableClient;
        this.notificationService = notificationService;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream();
    }

    private void initOrderStream() {
        log.info("Starting initOrderStream ... ");

        //Retrieve last resumeToken for watching collection at specific operation
        String resumeToken = null;

        if (!ConfigUtils.getProfiles().contains("test")) {
            try {
                TableEntity cdcStartAtEntity = tableClient.getEntity(CdcStartAtConstant.CDC_START_AT_PARTITION_KEY, CdcStartAtConstant.CDC_START_AT_ROW_KEY);
                if (Objects.nonNull(cdcStartAtEntity))
                    resumeToken = (String) cdcStartAtEntity.getProperty(CdcStartAtConstant.CDC_START_AT_PROPERTY);
            } catch (TableServiceException e) {
                log.warn("Table StartAt not found, it is starting from now ...");
            }
        }

        // Initialize watching collection
        ReactiveMongoCollection<Onboarding> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
        if (Objects.nonNull(resumeToken))
            options = options.resumeAfter(BsonDocument.parse(resumeToken));

        Bson match = Aggregates.match(Filters.and(
                Filters.in("operationType", asList("update", "replace", "insert")),
                Filters.in("fullDocument.status", Arrays.asList(COMPLETED.name(), DELETED.name()))));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<Onboarding>> publisher = dataCollection.watch(pipeline, Onboarding.class, options);
        publisher.subscribe().with(
                this::consumerOnboardingEvent,
                failure -> {
                    log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
                    constructMapAndTrackEvent(failure.getClass().toString(), "FALSE", ONBOARDING_FAILURE_MECTRICS);
                    Quarkus.asyncExit();
                });

        log.info("Completed initOrderStream ... ");
    }

    private ReactiveMongoCollection<Onboarding> getCollection() {
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, Onboarding.class);
    }

    protected void consumerOnboardingEvent(ChangeStreamDocument<Onboarding> document) {
        assert document.getFullDocument() != null;
        assert document.getDocumentKey() != null;

        log.info("Starting consumerOnboardingEvent ... ");
        log.info("Sending Onboarding notification having id {}", document.getFullDocument().getId());

        notificationService.invokeNotificationApi(document.getFullDocument())
                .subscribe().with(
                        result -> {
                            log.info("Onboarding notification having id: {} successfully sent", document.getDocumentKey().toJson());
                            updateLastResumeToken(document.getResumeToken());
                            constructMapAndTrackEvent(document.getDocumentKey().toJson(), "TRUE", ONBOARDING_SUCCESS_MECTRICS);
                        },
                        failure -> {
                            log.error("Error during send Onboarding notifiction having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
                            constructMapAndTrackEvent(document.getDocumentKey().toJson(), "FALSE", ONBOARDING_FAILURE_MECTRICS);
                        });
        log.info("End consumerOnboardingEvent ... ");
    }

    private void updateLastResumeToken(BsonDocument resumeToken) {
        // Table CdCStartAt will be updated with the last resume token
        Map<String, Object> properties = new HashMap<>();
        properties.put(CdcStartAtConstant.CDC_START_AT_PROPERTY, resumeToken.toJson());

        TableEntity tableEntity = new TableEntity(CdcStartAtConstant.CDC_START_AT_PARTITION_KEY, CdcStartAtConstant.CDC_START_AT_ROW_KEY)
                .setProperties(properties);
        tableClient.upsertEntity(tableEntity);

    }

    private void constructMapAndTrackEvent(String documentKey, String success, String... metrics) {
        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("documentKey", documentKey);
        propertiesMap.put("success", success);

        Map<String, Double> metricsMap = new HashMap<>();
        Arrays.stream(metrics).forEach(metricName -> metricsMap.put(metricName, 1D));
        telemetryClient.trackEvent(EVENT_NAME, propertiesMap, metricsMap);
    }
}

