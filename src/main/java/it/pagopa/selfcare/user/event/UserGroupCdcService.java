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
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.entity.UserGroupEntity;
import it.pagopa.selfcare.user.event.mapper.UserGroupNotificationMapper;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.UserGroupNotificationToSend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static it.pagopa.selfcare.user.UserUtils.mapPropsForTrackEvent;
import static it.pagopa.selfcare.user.event.constant.CdcStartAtConstant.*;
import static it.pagopa.selfcare.user.model.TrackEventInput.toTrackEventInputForUserGroup;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.*;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_GROUP_CDC_NAME;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class UserGroupCdcService {
    private static final String OPERATION_NAME = "USER-GROUP-CDC-sendUserGroupEvent";
    private static final String COLLECTION_NAME = "userGroups";

    private final TelemetryClient telemetryClient;
    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;
    private final TableClient tableClient;
    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;

    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;

    private final UserGroupNotificationMapper userGroupNotificationMapper;

    public UserGroupCdcService(ReactiveMongoClient mongoClient,
                               @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                               @ConfigProperty(name = "user-group-cdc.retry.min-backoff") Integer retryMinBackOff,
                               @ConfigProperty(name = "user-group-cdc.retry.max-backoff") Integer retryMaxBackOff,
                               @ConfigProperty(name = "user-group-cdc.retry") Integer maxRetry,
                               @ConfigProperty(name = "user-group-cdc.send-events.watch.enabled") Boolean sendEventsEnabled,
                               TelemetryClient telemetryClient,
                               TableClient tableClient,

                               UserGroupNotificationMapper userGroupNotificationMapper) {
        this.maxRetry = maxRetry;
        this.mongoClient = mongoClient;
        this.mongodbDatabase = mongodbDatabase;
        this.retryMaxBackOff = retryMaxBackOff;
        this.retryMinBackOff = retryMinBackOff;
        this.telemetryClient = telemetryClient;
        this.userGroupNotificationMapper = userGroupNotificationMapper;
        this.tableClient = tableClient;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream(sendEventsEnabled);
    }

    private void initOrderStream(Boolean sendEventsEnabled) {
        log.info("Starting initOrderStream ... ");

        String resumeToken = null;

        if (!ConfigUtils.getProfiles().contains("test")) {
            try {
                TableEntity cdcStartAtEntity = tableClient.getEntity(CDC_START_AT_PARTITION_KEY, CDC_START_AT_ROW_KEY);
                if (Objects.nonNull(cdcStartAtEntity))
                    resumeToken = (String) cdcStartAtEntity.getProperty(CDC_START_AT_PROPERTY);
            } catch (TableServiceException e) {
                log.warn("Table StarAt not found, it is starting from now ...");
            }
        }

        ReactiveMongoCollection<UserGroupEntity> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
        if (Objects.nonNull(resumeToken))
            options = options.resumeAfter(BsonDocument.parse(resumeToken));

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<UserGroupEntity>> publisher = dataCollection.watch(pipeline, UserGroupEntity.class, options);

        if (sendEventsEnabled) {
            publisher.subscribe().with(
                    this::consumerToSendScUserGroupEvent,
                    failure -> {
                        log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
                        telemetryClient.trackEvent(EVENT_USER_GROUP_CDC_NAME, mapPropsForTrackEvent(TrackEventInput.builder().exception(failure.getClass().toString()).build()), Map.of(EVENTS_USER_GROUP_FAILURE, 1D));
                        Quarkus.asyncExit();
                    });
        }

        log.info("Completed initOrderStream ... ");
    }

    private ReactiveMongoCollection<UserGroupEntity> getCollection() {
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, UserGroupEntity.class);
    }

    public void consumerToSendScUserGroupEvent(ChangeStreamDocument<UserGroupEntity> document) {

        assert document.getFullDocument() != null;
        assert document.getDocumentKey() != null;
        UserGroupEntity userGroupChanged = document.getFullDocument();

        log.info("Starting consumerToSendScUserGroupEvent ... ");

        UserGroupNotificationToSend userGroupNotificationToSend = userGroupNotificationMapper.toUserGroupNotificationToSend(userGroupChanged);

        eventHubRestClient.sendUserGroupMessage(userGroupNotificationToSend)
                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                .onItem().invoke(() -> telemetryClient.trackEvent(EVENT_USER_GROUP_CDC_NAME,
                        mapPropsForTrackEvent(toTrackEventInputForUserGroup(userGroupNotificationToSend)), Map.of(EVENTS_USER_GROUP_PRODUCT_SUCCESS, 1D)))
                .onFailure().invoke(() -> telemetryClient.trackEvent(EVENT_USER_GROUP_CDC_NAME,
                        mapPropsForTrackEvent(toTrackEventInputForUserGroup(userGroupNotificationToSend)), Map.of(EVENTS_USER_GROUP_PRODUCT_FAILURE, 1D)))
                .subscribe().with(
                        result -> {
                            log.info("SendEvents successfully performed from user group document having id: {}", document.getDocumentKey().toJson());
                            telemetryClient.trackEvent(EVENT_USER_GROUP_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputForUserGroup(userGroupNotificationToSend)), Map.of(EVENTS_USER_GROUP_SUCCESS, 1D));
                        },
                        failure -> {
                            log.error("Error during SendEvents from user group document having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
                            telemetryClient.trackEvent(EVENT_USER_GROUP_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputForUserGroup(userGroupNotificationToSend)), Map.of(EVENTS_USER_GROUP_FAILURE, 1D));
                        });
    }

}
