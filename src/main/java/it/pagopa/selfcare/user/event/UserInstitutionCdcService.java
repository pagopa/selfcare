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
import it.pagopa.selfcare.user.client.EventHubFdRestClient;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.mapper.NotificationMapper;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import it.pagopa.selfcare.user.model.NotificationUserType;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD_GARANTITO;
import static it.pagopa.selfcare.user.UserUtils.mapPropsForTrackEvent;
import static it.pagopa.selfcare.user.event.constant.CdcStartAtConstant.*;
import static it.pagopa.selfcare.user.model.TrackEventInput.toTrackEventInput;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.*;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_CDC_NAME;
import static it.pagopa.selfcare.user.model.constants.EventsName.FD_EVENT_USER_CDC_NAME;
import static java.util.Arrays.asList;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@Startup
@Slf4j
@ApplicationScoped
public class UserInstitutionCdcService {

    private static final String COLLECTION_NAME = "userInstitutions";
    private static final String OPERATION_NAME = "USER-CDC-UserInfoUpdate";
    public static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";
    public static final String ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE = "Error during subscribe collection, exception: {} , message: {}";


    private final TelemetryClient telemetryClient;

    private final TableClient tableClient;
    private final String mongodbDatabase;
    private final ReactiveMongoClient mongoClient;
    private final UserInstitutionRepository userInstitutionRepository;

    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;
    private final boolean sendEventsEnabled;
    private final boolean sendFdEventsEnabled;


    @RestClient
    @Inject
    UserApi userRegistryApi;
    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;

    @RestClient
    @Inject
    EventHubFdRestClient eventHubFdRestClient;

    private final NotificationMapper notificationMapper;


    public UserInstitutionCdcService(ReactiveMongoClient mongoClient,
                                     @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                                     @ConfigProperty(name = "user-cdc.retry.min-backoff") Integer retryMinBackOff,
                                     @ConfigProperty(name = "user-cdc.retry.max-backoff") Integer retryMaxBackOff,
                                     @ConfigProperty(name = "user-cdc.retry") Integer maxRetry,
                                     @ConfigProperty(name = "user-cdc.send-events.watch.enabled") Boolean sendEventsEnabled,
                                     @ConfigProperty(name = "user-cdc.send-events-fd.watch.enabled") Boolean sendFdEventsEnabled,
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
        this.sendEventsEnabled = sendEventsEnabled;
        this.sendFdEventsEnabled = sendFdEventsEnabled;
        telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        initOrderStream();
    }

    private void initOrderStream() {
        log.info("Starting initOrderStream ... ");

        //Retrieve last resumeToken for watching collection at specific operation
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

        // Initialize watching collection
        ReactiveMongoCollection<UserInstitution> dataCollection = getCollection();
        ChangeStreamOptions options = new ChangeStreamOptions()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
        if (Objects.nonNull(resumeToken))
            options = options.resumeAfter(BsonDocument.parse(resumeToken));

        Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
        Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
        List<Bson> pipeline = Arrays.asList(match, project);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = dataCollection.watch(pipeline, UserInstitution.class, options);
        publisher.subscribe().with(
                document -> propagateDocumentToConsumers(document, publisher),
                failure -> {
                    log.error(ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE, failure.toString(), failure.getMessage());
                    telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(TrackEventInput.builder().exception(failure.getClass().toString()).build()), Map.of(USER_INFO_UPDATE_FAILURE, 1D));
                    Quarkus.asyncExit();
                });
        log.info("Completed initOrderStream ... ");
    }

    /**
     *
     This method acts as a gateway to direct the modified entity to the correct consumers based on the following logic:
     if the modification concerns the user's email, it will invoke the consumer to send events to both the sc-user queue and the selfcare-fd queue,
     but the latter only if the entity contains at least one active product from prod-fd or prod-fd-garantito. If, however, the modification concerns one
     of the products within the entity, all three consumers will be invoked (two consumers for the events and the consumer for updating the userinfo collection).
     */
    public void propagateDocumentToConsumers(ChangeStreamDocument<UserInstitution> document, Multi<ChangeStreamDocument<UserInstitution>> publisher) {
        assert document.getFullDocument() != null;
        assert document.getDocumentKey() != null;
        UserInstitution userInstitutionChanged = document.getFullDocument();

        boolean hasActiveFdProduct = userInstitutionChanged.getProducts().stream()
                .anyMatch(product -> (PROD_FD.getValue().equals(product.getProductId()) || PROD_FD_GARANTITO.getValue().equals(product.getProductId()))
                        && OnboardedProductState.ACTIVE.equals(product.getStatus()));

        boolean userMailIsChanged = isUserMailChanged(userInstitutionChanged);

        if (Boolean.FALSE.equals(userMailIsChanged)) {
            publisher.subscribe().with(
                    this::consumerUserInstitutionRepositoryEvent,
                    failure -> {
                        log.error(ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE, failure.toString(), failure.getMessage());
                        telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(TrackEventInput.builder().exception(failure.getClass().toString()).build()), Map.of(USER_INFO_UPDATE_FAILURE, 1D));
                        Quarkus.asyncExit();
                    });
        }

        if (Boolean.TRUE.equals(sendEventsEnabled)) {
            publisher.subscribe().with(
                    this::consumerToSendScUserEvent,
                    failure -> {
                        log.error(ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE, failure.toString(), failure.getMessage());
                        telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(TrackEventInput.builder().exception(failure.getClass().toString()).build()), Map.of(EVENTS_USER_INSTITUTION_FAILURE, 1D));
                        Quarkus.asyncExit();
                    });
        }

        if (Boolean.TRUE.equals(sendFdEventsEnabled) && hasActiveFdProduct) {
            publisher.subscribe().with(
                    subscription -> consumerToSendUserEventForFD(document, userMailIsChanged),
                    failure -> {
                        log.error(ERROR_DURING_SUBSCRIBE_COLLECTION_EXCEPTION_MESSAGE, failure.toString(), failure.getMessage());
                        telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(TrackEventInput.builder().exception(failure.getClass().toString()).build()), Map.of(EVENTS_USER_INSTITUTION_FAILURE, 1D));
                        Quarkus.asyncExit();
                    });
        }
    }

    private boolean isUserMailChanged(UserInstitution userInstitutionChanged) {
        OffsetDateTime maxProductUpdateAt = null;
        if (Objects.nonNull(userInstitutionChanged.getProducts()) && !userInstitutionChanged.getProducts().isEmpty()) {
            maxProductUpdateAt = userInstitutionChanged.getProducts().stream()
                    .max(Comparator.comparing(OnboardedProduct::getUpdatedAt, nullsLast(naturalOrder())))
                    .map(OnboardedProduct::getUpdatedAt)
                    .orElse(null);
        }
        OffsetDateTime maxUserMailUpdateAt = userInstitutionChanged.getUserMailUpdatedAt();
        return Objects.nonNull(maxProductUpdateAt) && Objects.nonNull(maxUserMailUpdateAt) && maxUserMailUpdateAt.isAfter(maxProductUpdateAt);
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

        log.info("Starting consumerUserInstitutionRepositoryEvent from UserInstitution document having id: {}", userInstitutionId);

        userInstitutionRepository.updateUser(userInstitutionChanged)
                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofHours(retryMaxBackOff)).atMost(maxRetry)
                .subscribe().with(
                        result -> {
                            log.info("UserInfo collection successfully updated from UserInstitution document having id: {}", userInstitutionId);
                            updateLastResumeToken(document.getResumeToken());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputByUserInstitution(userInstitutionChanged)), Map.of(USER_INFO_UPDATE_SUCCESS, 1D));
                        },
                        failure -> {
                            log.error("Error during UserInfo collection updating, from UserInstitution document having id: {} , message: {}", userInstitutionId, failure.getMessage());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputByUserInstitution(userInstitutionChanged)), Map.of(USER_INFO_UPDATE_FAILURE, 1D));
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

        log.info("Starting consumerToSendScUserEvent ... ");

        userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitutionChanged.getUserId())
                .onFailure(this::checkIfIsRetryableException)
                .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                .onItem().transformToUni(userResource -> Multi.createFrom().iterable(UserUtils.groupingProductAndReturnMinStateProduct(userInstitutionChanged.getProducts()))
                        .map(onboardedProduct -> notificationMapper.toUserNotificationToSend(userInstitutionChanged, onboardedProduct, userResource))
                        .onItem().transformToUniAndMerge(userNotificationToSend -> eventHubRestClient.sendMessage(userNotificationToSend)
                                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                                .onItem().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(userNotificationToSend)), Map.of(EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS, 1D)))
                                .onFailure().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(userNotificationToSend)), Map.of(EVENTS_USER_INSTITUTION_PRODUCT_FAILURE, 1D))))
                        .toUni()
                )
                .subscribe().with(
                        result -> {
                            log.info("SendEvents successfully performed from UserInstitution document having id: {}", document.getDocumentKey().toJson());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputByUserInstitution(userInstitutionChanged)), Map.of(EVENTS_USER_INSTITUTION_SUCCESS, 1D));
                        },
                        failure -> {
                            log.error("Error during SendEvents from UserInstitution document having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
                            telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputByUserInstitution(userInstitutionChanged)), Map.of(EVENTS_USER_INSTITUTION_FAILURE, 1D));
                        });
    }


    /**
     *
     This method handles the sending of events for the selfcare fd topic.
     In case the modification concerns the user's email, it sends two events: the first of type DELETE and the second of type ACTIVE.
     On the other hand, if the modification relates to the activation, suspension, reactivation, or cancellation of the prod-fd or prod-fd-garantito product,
     it will send a single event indicating the type of action performed.
     */
    public void consumerToSendUserEventForFD(ChangeStreamDocument<UserInstitution> document, boolean isUserMailChanged) {

        if (Objects.nonNull(document.getFullDocument()) && Objects.nonNull(document.getDocumentKey())) {
            UserInstitution userInstitutionChanged = document.getFullDocument();

            log.info("Starting consumerToSendUserEventForFd ... ");

            userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitutionChanged.getUserId())
                    .onFailure(this::checkIfIsRetryableException)
                    .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                    .onItem().transformToMulti(userResource -> Multi.createFrom().iterable(UserUtils.retrieveFdProduct(userInstitutionChanged.getProducts(), List.of(PROD_FD.getValue(), PROD_FD_GARANTITO.getValue()), isUserMailChanged))
                            .onItem().transformToUniAndMerge(onboardedProduct -> {
                                        FdUserNotificationToSend fdUserNotificationToSend = notificationMapper.toFdUserNotificationToSend(userInstitutionChanged, onboardedProduct, userResource, evaluateType(onboardedProduct));
                                        log.info("Sending message to EventHubFdRestClient ... ");
                                        if (isUserMailChanged && NotificationUserType.ACTIVE_USER.equals(fdUserNotificationToSend.getType())) {
                                            log.info("User mail is changed, sending DELETE_USER event first ... ");
                                            FdUserNotificationToSend fdUserNotificationToSendDelete = notificationMapper.toFdUserNotificationToSend(userInstitutionChanged, onboardedProduct, userResource, NotificationUserType.DELETE_USER);
                                            return eventHubFdRestClient.sendMessage(fdUserNotificationToSendDelete)
                                                    .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                                                    .onItem().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(fdUserNotificationToSendDelete)), Map.of(FD_EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS, 1D)))
                                                    .onFailure().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(fdUserNotificationToSendDelete)), Map.of(FD_EVENTS_USER_INSTITUTION_PRODUCT_FAILURE, 1D)))
                                                    .onItem().transformToUni(unused -> eventHubFdRestClient.sendMessage(fdUserNotificationToSend))
                                                    .onItem().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(fdUserNotificationToSend)), Map.of(FD_EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS, 1D)))
                                                    .onFailure().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(fdUserNotificationToSend)), Map.of(FD_EVENTS_USER_INSTITUTION_PRODUCT_FAILURE, 1D)));
                                        }
                                        return eventHubFdRestClient.sendMessage(fdUserNotificationToSend)
                                                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry)
                                                .onItem().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(fdUserNotificationToSend)), Map.of(FD_EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS, 1D)))
                                                .onFailure().invoke(() -> telemetryClient.trackEvent(EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInput(fdUserNotificationToSend)), Map.of(FD_EVENTS_USER_INSTITUTION_PRODUCT_FAILURE, 1D)));
                                    }
                            ))
                    .subscribe().with(
                            result -> {
                                log.info("SendFdEvents successfully performed from UserInstitution document having id: {}", document.getDocumentKey().toJson());
                                telemetryClient.trackEvent(FD_EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputByUserInstitution(userInstitutionChanged)), Map.of(FD_EVENTS_USER_INSTITUTION_SUCCESS, 1D));
                            },
                            failure -> {
                                log.error("Error during SendFdEvents from UserInstitution document having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
                                telemetryClient.trackEvent(FD_EVENT_USER_CDC_NAME, mapPropsForTrackEvent(toTrackEventInputByUserInstitution(userInstitutionChanged)), Map.of(FD_EVENTS_USER_INSTITUTION_FAILURE, 1D));
                            });
        }
    }

    private NotificationUserType evaluateType(OnboardedProduct onboardedProduct) {
        return switch (onboardedProduct.getStatus()) {
            case ACTIVE -> NotificationUserType.ACTIVE_USER;
            case SUSPENDED -> NotificationUserType.SUSPEND_USER;
            case DELETED -> NotificationUserType.DELETE_USER;
            default -> null;
        };
    }

    private boolean checkIfIsRetryableException(Throwable throwable) {
        return throwable instanceof TimeoutException ||
                (throwable instanceof ClientWebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == 429);
    }

    private TrackEventInput toTrackEventInputByUserInstitution(UserInstitution userInstitution) {
        return TrackEventInput.builder()
                .documentKey(userInstitution.getId().toHexString())
                .userId(userInstitution.getUserId())
                .institutionId(userInstitution.getInstitutionId())
                .build();
    }
}
