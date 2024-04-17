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
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonTimestamp;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static it.pagopa.selfcare.user.event.constant.CdcStartAtConstant.*;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class UserInstitutionCdcService {

    private static final String COLLECTION_NAME = "userInstitutions";
    private static final String OPERATION_NAME = "USER-CDC-UserInfoUpdate";
    private static final String EVENT_NAME = "USER-CDC";
    private static final String USERINSTITUTION_FAILURE_MECTRICS = "UserInfoUpdate_failures";
    private static final String USERINSTITUTION_SUCCESS_MECTRICS = "UserInfoUpdate_successes";

    private final TelemetryClient telemetryClient;

    private final TableClient tableClient;
    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;
    private final UserInstitutionRepository userInstitutionRepository;

    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;


    public UserInstitutionCdcService(ReactiveMongoClient mongoClient,
                                     @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                                     @ConfigProperty(name = "user-cdc.retry.min-backoff") Integer retryMinBackOff,
                                     @ConfigProperty(name = "user-cdc.retry.max-backoff") Integer retryMaxBackOff,
                                     @ConfigProperty(name = "user-cdc.retry") Integer maxRetry,
                                     UserInstitutionRepository userInstitutionRepository,
                                     TelemetryClient telemetryClient,
                                     TableClient tableClient) {
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.userInstitutionRepository = userInstitutionRepository;
        this.maxRetry = maxRetry;
        this.retryMaxBackOff = retryMaxBackOff;
        this.retryMinBackOff = retryMinBackOff;
        this.telemetryClient = telemetryClient;
        this.tableClient = tableClient;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream();
    }

    private void initOrderStream() {
        log.info("Starting initOrderStream ... ");

        //Handling startAtOperationTime for watching collection at specific time
        long startAtLong = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try {
            TableEntity cdcStartAtEntity = tableClient.getEntity(CDC_START_AT_PARTITION_KEY, CDC_START_AT_ROW_KEY);
            if(cdcStartAtEntity == null)
                startAtLong = (Long) cdcStartAtEntity.getProperty(CDC_START_AT_PROPERTY);
        } catch (TableServiceException e) {
            log.error("Table StarAt not found, it is starting from now ...");
        }

        ReactiveMongoCollection<UserInstitution> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .startAtOperationTime(new BsonTimestamp(startAtLong));

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = dataCollection.watch(pipeline, UserInstitution.class, options);
        publisher.subscribe().with(
                this::consumerUserInstitutionRepositoryEvent,
                failure -> {
                    log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
                    constructMapAndTrackEvent(failure.getClass().toString(), "FALSE", USERINSTITUTION_FAILURE_MECTRICS);
                    Quarkus.asyncExit();
                });

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

        log.info("Starting consumerUserInstitutionRepositoryEvent ... ");

        userInstitutionRepository.updateUser(document.getFullDocument())
                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofHours(retryMaxBackOff)).atMost(maxRetry)
                .subscribe().with(
                        result -> {
                            log.info("UserInfo collection successfully updated from UserInstitution document having id: {}", document.getDocumentKey().toJson());
                            constructMapAndTrackEvent(document.getDocumentKey().toJson(), "TRUE", USERINSTITUTION_SUCCESS_MECTRICS);
                        },
                        failure -> {
                            log.error("Error during UserInfo collection updating, from UserInstitution document having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
                            constructMapAndTrackEvent(document.getDocumentKey().toJson(), "FALSE", USERINSTITUTION_FAILURE_MECTRICS);
                        });
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
