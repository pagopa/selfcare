package it.pagopa.selfcare.user.event;

import com.microsoft.applicationinsights.TelemetryClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.*;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
@IfBuildProperty(name = "user-cdc.mongodb.watch.enabled", stringValue = "true")
public class UserInstitutionCdcService {

    private static final String COLLECTION_NAME = "userInstitutions";
    private static final String OPERATION_NAME = "USER-CDC-UserInfoUpdate";
    private static final String EVENT_NAME = "USER-CDC";
    private static final String USERINSTITUTION_FAILURE_MECTRICS = "UserInfoUpdate_failures";
    private static final String USERINSTITUTION_SUCCESS_MECTRICS = "UserInfoUpdate_successes";

    private final TelemetryClient telemetryClient;
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
                                     TelemetryClient telemetryClient) {
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.userInstitutionRepository = userInstitutionRepository;
        this.maxRetry = maxRetry;
        this.retryMaxBackOff = retryMaxBackOff;
        this.retryMinBackOff = retryMinBackOff;
        this.telemetryClient = telemetryClient;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream();
    }

    private void initOrderStream() {
        ReactiveMongoCollection<UserInstitution> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions().fullDocument(FullDocument.UPDATE_LOOKUP);

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = dataCollection.watch(pipeline, UserInstitution.class, options);
        publisher.subscribe().with(this::consumerUserInstitutionRepositoryEvent);
    }

    private ReactiveMongoCollection<UserInstitution> getCollection() {
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, UserInstitution.class);
    }

    protected void consumerUserInstitutionRepositoryEvent(ChangeStreamDocument<UserInstitution> document) {

        assert document.getFullDocument() != null;
        assert document.getDocumentKey() != null;

        userInstitutionRepository.updateUser(document.getFullDocument())
                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofHours(retryMaxBackOff)).atMost(maxRetry)
                .subscribe().with(
                        result -> {
                            constructMapAndTrackEvent(document.getDocumentKey().toJson(), "TRUE", USERINSTITUTION_SUCCESS_MECTRICS);
                            log.info("UserInfo collection successfully updated from UserInstitution document having id: {}", document.getDocumentKey().toJson());
                        },
                        failure -> {
                            constructMapAndTrackEvent(document.getDocumentKey().toJson(), "FALSE", USERINSTITUTION_FAILURE_MECTRICS);
                            log.error("Error during UserInfo collection updating, from UserInstitution document having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
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
