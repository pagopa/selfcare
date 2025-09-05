package it.pagopa.selfcare.user.service;

import com.microsoft.applicationinsights.TelemetryClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.*;
import it.pagopa.selfcare.user.controller.response.product.OnboardedProductWithActions;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.exception.UserRoleAlreadyPresentException;
import it.pagopa.selfcare.user.mapper.OnboardedProductMapper;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.model.constants.QueueEvent;
import it.pagopa.selfcare.user.model.notification.PrepareNotificationData;
import it.pagopa.selfcare.user.service.utils.CreateOrUpdateUserByFiscalCodeResponse;
import it.pagopa.selfcare.user.service.utils.OPERATION_TYPE;
import it.pagopa.selfcare.user.util.ActionMapRetriever;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;
import org.owasp.encoder.Encode;
import software.amazon.awssdk.utils.CollectionUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.pagopa.selfcare.user.UserUtils.mapPropsForTrackEvent;
import static it.pagopa.selfcare.user.constant.CollectionUtil.MAIL_ID_PREFIX;
import static it.pagopa.selfcare.user.constant.CustomError.*;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_FAILURE;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_SUCCESS;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_MS_NAME;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.*;
import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;
import static it.pagopa.selfcare.user.util.UserUtils.VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @ConfigProperty(name = "user-ms.eventhub.users.concurrency-level")
    Integer eventhubUsersConcurrencyLevel;

    private final UserRegistryService userRegistryService;

    private final UserMapper userMapper;
    private final UserInstitutionMapper userInstitutionMapper;
    private final OnboardedProductMapper onboardedProductMapper;

    private final UserUtils userUtils;

    private final ProductService productService;
    private final UserInstitutionService userInstitutionService;
    private final UserNotificationService userNotificationService;
    private final TelemetryClient telemetryClient;
    private final ActionMapRetriever actionMapRetriever;

    Supplier<String> randomMailId = () -> (MAIL_ID_PREFIX + UUID.randomUUID());
    private static final String WORK_CONTACTS = "workContacts";
    static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,email,name,workContacts";
    public static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";
    public static final String USER_FIELD_LIST_WITHOUT_WORK_CONTACTS = "name,familyName,fiscalCode";
    private static final String USER_INSTITUTION_FOUNDED = "UserInstitution with userId: {} and institutionId: {} founded";
    private static final String USER_INSTITUTION_NOT_FOUND = "UserInstitution with userId: {} and institutionId: {} not found";

    /**
     * The updateUserStatus function updates the status of a user's onboarded product.
     */
    @Override
    public Uni<Void> updateUserStatusWithOptionalFilter(String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status) {

        if (status == null) {
            return Uni.createFrom().failure(new InvalidRequestException(STATUS_IS_MANDATORY.getMessage()));
        }

        return userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, role, productRole, status)
                .onItem().transformToUni(aLong -> {
                    if (aLong < 1) {
                        return Uni.createFrom().failure(new ResourceNotFoundException(USER_TO_UPDATE_NOT_FOUND.getMessage()));
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @Override
    public Uni<List<String>> getUsersEmails(String institutionId, String productId) {
        var userInstitutionFilters = UserInstitutionFilter.builder().institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(productId).status(ACTIVE).build().constructMap();
        Multi<UserInstitution> userInstitutions = userInstitutionService.findAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters));
        return userInstitutions
                .onItem().transformToUni(userInstitution -> userRegistryService.findByIdUsingGET(WORK_CONTACTS, userInstitution.getUserId())
                        .map(userResource -> Objects.nonNull(userResource.getWorkContacts()) && userResource.getWorkContacts().containsKey(userInstitution.getUserMailUuid())
                                ? userResource.getWorkContacts().get(userInstitution.getUserMailUuid()) : null))
                .merge()
                .filter(workContactResource -> Objects.nonNull(workContactResource) && StringUtils.isNotBlank(workContactResource.getEmail().getValue()))
                .map(workContactResource -> workContactResource.getEmail().getValue())
                .collect().asList();

    }

    @Override
    public Multi<UserProductResponse> getUserProductsByInstitution(String institutionId) {
        Multi<UserInstitution> userInstitutions = UserInstitution.find(UserInstitution.Fields.institutionId.name(), institutionId).stream();
        return userInstitutions.onItem()
                .transformToUni(userInstitution -> userRegistryService.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                        .map(userResource -> UserProductResponse.builder()
                                .id(userResource.getId().toString())
                                .name(userResource.getName().getValue())
                                .surname(userResource.getFamilyName().getValue())
                                .taxCode(userResource.getFiscalCode())
                                .products(onboardedProductMapper.toList(userInstitution.getProducts()))
                                .email(UserUtils.getMailByMailUuid(userResource.getWorkContacts(), userInstitution.getUserMailUuid()).orElse(null))
                                .build()))
                .merge();
    }

    @Override
    public Uni<UserResponse> retrievePerson(String userId, String productId, String institutionId) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(() -> {
                    log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId));
                    return new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode());
                })
                .onItem().transformToUni(userInstitution -> userRegistryService.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                    .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode()))
                    .onItem().transform(user -> userMapper.toUserResponse(user, userInstitution.getUserMailUuid()))
                );
    }

    @Override
    public Multi<UserInstitutionResponse> findAllUserInstitutions(String institutionId, String userId, List<String> roles, List<String> states, List<String> products, List<String> productRoles) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(products).status(states).role(roles).productRole(productRoles).build().constructMap();
        return userInstitutionService.findAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters))
                .onItem().transform(filterAndSetProducts(roles, states, products, productRoles))
                .onItem().transform(userInstitutionMapper::toResponse);
    }

    @Override
    public Multi<UserInstitutionResponse> findPaginatedUserInstitutions(String institutionId, String userId, List<PartyRole> roles, List<String> states, List<String> products, List<String> productRoles, Integer page, Integer size) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(products).status(states).role(roles).productRole(productRoles).build().constructMap();
        var rolesString = Optional.ofNullable(roles).map(items -> items.stream().map(PartyRole::name).toList()).orElse(null);
        return userInstitutionService.paginatedFindAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters), page, size)
                .onItem().transform(filterAndSetProducts(rolesString, states, products, productRoles))
                .onItem().transform(userInstitutionMapper::toResponse);
    }

    private static Function<UserInstitution, UserInstitution> filterAndSetProducts(List<String> roles, List<String> states, List<String> products, List<String> productRoles) {

        return userInstitution -> {
            userInstitution.setProducts(userInstitution.getProducts().stream()
                    .filter(product -> CollectionUtils.isNullOrEmpty(products) || products.contains(product.getProductId()))
                    .filter(product -> CollectionUtils.isNullOrEmpty(states) || states.contains(Optional.ofNullable(product.getStatus()).map(OnboardedProductState::name).orElse(null)))
                    .filter(product -> CollectionUtils.isNullOrEmpty(roles) || roles.contains(Optional.ofNullable(product.getRole()).map(PartyRole::name).orElse(null)))
                    .filter(product -> CollectionUtils.isNullOrEmpty(productRoles) || productRoles.contains(product.getProductRole()))
                    .toList());
            return userInstitution;
        };
    }

    @Override
    public Uni<Void> deleteUserInstitutionProduct(String userId, String institutionId, String productId) {
        return userInstitutionService.deleteUserInstitutionProduct(userId, institutionId, productId)
                .onItem().transformToUni(aLong -> {
                    if (aLong < 1) {
                        return Uni.createFrom().failure(new ResourceNotFoundException(USER_TO_UPDATE_NOT_FOUND.getMessage()));
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @Override
    public Uni<DeletedUserCountResponse> deleteUserInstitutionProductUsers(String institutionId, String productId) {
        return userInstitutionService.deleteUserInstitutionProductUsers(institutionId, productId)
                .onItem().invoke(modCount -> log.info("Updated {} userInstitution documents with institutionId {} and productId {} to status DELETED", modCount, institutionId, productId))
                .onItem().transformToUni(modCount -> Uni.createFrom().item(new DeletedUserCountResponse(institutionId, productId, modCount)));
    }

    @Override
    public Uni<List<UserInstitutionResponse>> findAllByIds(List<String> userIds) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(formatQueryParameterList(userIds)).build().constructMap();
        return userInstitutionService.findAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters))
                .collect()
                .asList().onItem().transform(userInstitutions -> userInstitutions.stream().map(userInstitutionMapper::toResponse).toList());
    }

    @Override
    public Uni<Void> updateUserProductCreatedAt(String institutionId, List<String> userIds, String productId, OffsetDateTime createdAt) {
        return userInstitutionService.updateUserCreatedAtByInstitutionAndProduct(institutionId, userIds, productId, createdAt)
                .onItem().transformToUni(aLong -> {
                    if (aLong < 1) {
                        return Uni.createFrom().failure(new ResourceNotFoundException(USERS_TO_UPDATE_NOT_FOUND.getMessage()));
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @Override
    public Uni<UserDetailResponse> getUserById(String userId, String institutionId, String fieldsToRetrieve) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var fields = StringUtils.isBlank(fieldsToRetrieve) ? USERS_WORKS_FIELD_LIST : fieldsToRetrieve;
        return userInstitutionService.retrieveFirstFilteredUserInstitution(userUtils.retrieveMapForFilter(userInstitutionFilters))
                .onItem().ifNull().continueWith(() -> {
                    log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId));
                    return new UserInstitution();
                })
                .onItem().transformToUni(userInstitution -> userRegistryService.findByIdUsingGET(fields, userId)
                        .map(userResource -> userMapper.toUserDetailResponse(userResource, Optional.ofNullable(institutionId).map(ignored -> userInstitution.getUserMailUuid()).orElse(null))))
                .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode()));
    }

    @Override
    public Uni<UserDetailResponse> searchUserByFiscalCode(String fiscalCode, String institutionId) {
        Uni<UserResource> userResourceUni = userRegistryService.searchUsingPOST(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, new UserSearchDto(fiscalCode))
                .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException("User not found", USER_NOT_FOUND_ERROR.getCode()));

        return userResourceUni
                .onItem()
                .transformToUni(userResource -> {
                    var userInstitutionFilters = UserInstitutionFilter.builder().userId(userResource.getId().toString()).institutionId(institutionId).build().constructMap();
                    Uni<String> userMailUuid = userInstitutionService.retrieveFirstFilteredUserInstitution(userUtils.retrieveMapForFilter(userInstitutionFilters))
                            .onItem().ifNull().continueWith(() -> {
                                log.debug(String.format(USER_NOT_FOUND_ERROR.getMessage(), userResource.getId()));
                                return new UserInstitution();
                            })
                            .map(UserInstitution::getUserMailUuid);
                    return userMailUuid.map(mailId -> userMapper.toUserDetailResponse(userResource, Optional.ofNullable(institutionId).map(ignored -> mailId).orElse(null)));
                })
                .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage()), USER_NOT_FOUND_ERROR.getCode()));
    }

    /**
     * The updateUserProductStatus method in the UserServiceImpl class is responsible for updating the status of a user's product and sending notifications about this update.
     * it calls the updateUserStatusWithOptionalFilter method to update the user's product status on UserInstitution collection.
     * After the status update, the method retrieves the user's data from the user registry and adds it to the PrepareNotificationDataBuilder.
     * Then, it retrieves the user's institution data from UserInstitituion Collection.
     * After that, it retrieves the product data.
     * Once all the necessary data is retrieved and added to the PrepareNotificationDataBuilder, which is used to build the data needed for sending notifications.
     * Next, it sends an email notification about the status update.
     * Then, it builds a UserNotificationToSend to send a Kafka notification about the status update.
     */
    @Override
    public Uni<Void> updateUserProductStatus(String userId, String institutionId, String productId, OnboardedProductState status, String productRole, LoggedUser loggedUser) {
        PrepareNotificationData.PrepareNotificationDataBuilder prepareNotificationDataBuilder = PrepareNotificationData.builder();
        return updateUserStatusWithOptionalFilter(userId, institutionId, productId, null, productRole, status)
                .onItem().transformToUni(unused -> retrieveUserFromUserRegistryAndAddToPrepareNotificationData(prepareNotificationDataBuilder, userId))
                .onItem().transformToUni(builder -> retrieveUserInstitutionAndAddToPrepareNotificationData(builder, userId, institutionId))
                .onItem().transformToUni(builder -> retrieveProductAndAddToPrepareNotificationData(builder, productId))
                .onItem().transform(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                .onItem().transformToUni(prepareNotificationData -> userNotificationService.sendEmailNotification(prepareNotificationData.getUserResource(), prepareNotificationData.getUserInstitution(), prepareNotificationData.getProduct(), status, productRole, loggedUser.getName(), loggedUser.getFamilyName())
                        .onFailure().recoverWithNull()
                        .replaceWith(prepareNotificationData))
                .onFailure().invoke(throwable -> log.error("Error during update user status for userId: {}, institutionId: {}, productId:{} -> exception: {}", userId, institutionId, productId, throwable.getMessage(), throwable))
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> sendMailUserRequest(String userId, String userMailUuid, String institutionName, String productId, PartyRole productRole, String loggedUserId) {
        PrepareNotificationData.PrepareNotificationDataBuilder prepareNotificationDataBuilder = PrepareNotificationData.builder();
        return Uni.createFrom().nullItem()
                .onItem().transformToUni(unused -> retrieveUserFromUserRegistryAndAddToPrepareNotificationData(prepareNotificationDataBuilder, userId))
                .onItem().transformToUni(unused -> retrieveLoggedUserFromUserRegistryAndAddToPrepareNotificationData(prepareNotificationDataBuilder, loggedUserId))
                .onItem().transformToUni(builder -> retrieveUserInstitutionAndAddToPrepareNotificationDataRequest(builder, userId, userMailUuid, institutionName))
                .onItem().transformToUni(builder -> retrieveProductAndAddToPrepareNotificationData(builder, productId))
                .onItem().transform(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                .onItem().transformToUni(prepareNotificationData -> userNotificationService.buildDataModelRequestAndSendEmail(prepareNotificationData.getUserResource(), prepareNotificationData.getUserInstitution(), prepareNotificationData.getProduct(), productRole, prepareNotificationData.getLoggedUserResource().getName().getValue(), prepareNotificationData.getLoggedUserResource().getFamilyName().getValue())
                        .onFailure().recoverWithNull()
                        .replaceWith(prepareNotificationData))
                .onFailure().invoke(throwable -> log.error("Error during update user status for userId: {}, institutionId: {}, productId:{} -> exception: {}", Encode.forJava(userId), Encode.forJava(institutionName), Encode.forJava(productId), throwable.getMessage(), throwable))
                .replaceWithVoid();
    }

    private Uni<PrepareNotificationData.PrepareNotificationDataBuilder> retrieveProductAndAddToPrepareNotificationData(PrepareNotificationData.PrepareNotificationDataBuilder builder, String productId) {
        return Uni.createFrom().item(productService.getProduct(productId))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(builder::product);
    }


    private Uni<PrepareNotificationData.PrepareNotificationDataBuilder> retrieveUserInstitutionAndAddToPrepareNotificationData(PrepareNotificationData.PrepareNotificationDataBuilder builder, String userId, String institutionId) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        return userInstitutionService.retrieveFirstFilteredUserInstitution(userInstitutionFilters)
                .onItemOrFailure().transformToUni((userInstitution, throwable) -> {
                    if (throwable != null) {
                        log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId));
                        return Uni.createFrom().failure(new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode()));
                    }
                    builder.userInstitution(userInstitution);
                    return Uni.createFrom().item(userInstitution);
                })
                .replaceWith(builder);
    }

    private Uni<PrepareNotificationData.PrepareNotificationDataBuilder> retrieveUserInstitutionAndAddToPrepareNotificationDataRequest(PrepareNotificationData.PrepareNotificationDataBuilder builder, String userId, String userMailUuid, String institutionName) {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId(userId);
        userInstitution.setUserMailUuid(userMailUuid);
        userInstitution.setInstitutionDescription(institutionName);
        builder.userInstitution(userInstitution);
        return Uni.createFrom().item(builder);
    }

    private Uni<PrepareNotificationData.PrepareNotificationDataBuilder> retrieveUserFromUserRegistryAndAddToPrepareNotificationData(PrepareNotificationData.PrepareNotificationDataBuilder prepareNotificationDataBuilder, String userId) {
        return userRegistryService.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userId)
                .onItem().transform(prepareNotificationDataBuilder::userResource);
    }

    private Uni<PrepareNotificationData.PrepareNotificationDataBuilder> retrieveLoggedUserFromUserRegistryAndAddToPrepareNotificationData(PrepareNotificationData.PrepareNotificationDataBuilder prepareNotificationDataBuilder, String userId) {
        return userRegistryService.findByIdUsingGET(USER_FIELD_LIST_WITHOUT_WORK_CONTACTS, userId)
                .onItem().transform(prepareNotificationDataBuilder::loggedUserResource);
    }

    @Override
    public Uni<List<UserNotificationToSend>> findPaginatedUserNotificationToSend(Integer size, Integer page, String productId) {
        Map<String, Object> queryParameter;
        if (StringUtils.isNotBlank(productId)) {
            queryParameter = OnboardedProductFilter.builder().productId(productId).status(VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION).build().constructMap();
        } else {
            queryParameter = OnboardedProductFilter.builder().status(VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION).build().constructMap();
        }
        return userInstitutionService.paginatedFindAllWithFilter(queryParameter, page, size)
                .onItem().transformToUniAndMerge(userInstitution -> userRegistryService
                        .findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId())
                        .map(userResource -> userUtils.buildUsersNotificationResponse(userInstitution, userResource, productId)))
                .collect().in(ArrayList::new, List::addAll);
    }

    @Override
    public Uni<UserInfo> retrieveBindings(String institutionId, String userId, String[] states) {
        String[] finalStates = states != null && states.length > 0 ? states : null;
        return UserInfo.findByIdOptional(userId)
                .map(opt -> opt.map(UserInfo.class::cast)
                        .map(userInfo -> {
                            UserInfo filteredUserInfo = userUtils.filterInstitutionRoles(userInfo, finalStates, institutionId);
                            if (filteredUserInfo.getInstitutions() == null || filteredUserInfo.getInstitutions().isEmpty()) {
                                throw new ResourceNotFoundException(String.format("User with id %s and institution id %s not found!", userId, institutionId));
                            }
                            return filteredUserInfo;
                        })
                        .orElseThrow(() -> new ResourceNotFoundException(String.format("User with id %s not found!", userId)))
                );
    }

    /**
     * The createOrUpdateUserByFiscalCode method is a method that either creates a new user or updates an existing one based on the provided CreateUserDto object.
     * The method starts by calling the searchUsingPOST method on the userRegistryService object, this is an asynchronous operation that searches for a user in the user registry.
     * If the search operation fails with a UserNotFoundException, it recovers by returning a Uni that fails with a ResourceNotFoundException.
     * This is a way of transforming one type of exception into another.
     * If the search operation is successful, it call method to Update user on userRegistry and UserInstitution collection.
     * In case of previously fail with ResourceNotFoundException, it recovers by calling the method to create new User on userRegsitry and UserInstitution
     * collection.
     * In both cases at the end of persistence it sends an email and events on kafka
     * Finally, if any operation fails, it logs an error message and returns a Uni that emits a failure.
     */
    @Override
    public Uni<CreateOrUpdateUserByFiscalCodeResponse> createOrUpdateUserByFiscalCode(CreateUserDto userDto, LoggedUser loggedUser) {
        return userRegistryService.searchUsingPOST(USERS_WORKS_FIELD_LIST, new UserSearchDto(userDto.getUser().getFiscalCode().trim()))
                .onFailure(UserUtils::isUserNotFoundExceptionOnUserRegistry).retry().atMost(2)
                .onFailure(UserUtils::isUserNotFoundExceptionOnUserRegistry).recoverWithUni(throwable -> Uni.createFrom().failure(new ResourceNotFoundException(throwable.getMessage())))
                .onItem().transformToUni(userResource -> updateUserOnUserRegistryAndUserInstitutionByFiscalCode(userResource, userDto,loggedUser))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> createUserOnUserRegistryAndUserInstitution(userDto,loggedUser))
                .onFailure().invoke(exception -> log.error("Error during retrieve user from userRegistry: {} ", exception.getMessage(), exception));

    }

    /**
     * The updateUserOnUserRegistryAndUserInstitution method is a method that updates a user's information on both the user registry and the user institution.
     * The method retrieves the mailUuid from the user's work contacts. If it doesn't exist, it generates a new one.
     * Next, it builds a workContact object using the institution email from the userDto object and adds it to the user's work contacts using the mailUuid as the key.
     * The method then attempts to update the user on the user registry using a PATCH request.
     * After the user registry update, the method attempts to find the related userInstitution by the user ID and institution ID from the userDto object.
     * Once the userInstitution model is updated or created, attempts to persist the user institution.
     * If any of the operations fail, the method logs an error message and returns a Uni that emits a failure.
     */
    private Uni<CreateOrUpdateUserByFiscalCodeResponse> updateUserOnUserRegistryAndUserInstitutionByFiscalCode(UserResource userResource, CreateUserDto userDto, LoggedUser loggedUser) {
        log.info("Updating user on userRegistry and userInstitution");
        String mailUuid = userUtils.getMailUuidFromMail(userResource.getWorkContacts(), userDto.getUser().getInstitutionEmail())
                .orElse(randomMailId.get());

        var workContact = UserUtils.buildWorkContact(userDto.getUser().getInstitutionEmail());

        // when update workContract on userRegistry we must create an empty map with only key that we want persist
        Map<String, WorkContactResource> workContactToSave = new HashMap<>();
        workContactToSave.put(mailUuid, workContact);
        userResource.setWorkContacts(workContactToSave);

        return userRegistryService.updateUsingPATCH(userResource.getId().toString(), userMapper.toMutableUserFieldsDto(userResource))
                .onFailure().invoke(exception -> log.error("Error during update user on userRegistry: {} ", exception.getMessage(), exception))
                .onItem().invoke(response -> log.info("User with id {} updated on userRegistry", userResource.getId()))
                .onItem().transformToUni(response -> userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), userDto.getInstitutionId()))
                .onItem().transformToUni(userInstitution -> updateOrCreateUserInstitution(userDto, mailUuid, userInstitution, userResource.getId().toString()))
                .onItem().ifNotNull().transformToUni(userInstitution -> userInstitutionService.persistOrUpdate(userInstitution)
                    .onItem().invoke(() -> log.info("UserInstitution persisted with userId:{}, institutionId:{}", userInstitution.getUserId(), userInstitution.getInstitutionId()))
                    .onItem().transformToUni(prepareNotificationData -> sendNotificationsAndReturnData(userInstitution, userResource, userDto.getProduct().getProductRoles(), userDto.getHasToSendEmail(), loggedUser,  userDto.getProduct().getProductId(), QueueEvent.UPDATE)
                        .map(ignore -> CreateOrUpdateUserByFiscalCodeResponse.builder()
                                .operationType(OPERATION_TYPE.CREATED_OR_UPDATED)
                                .userId(userResource.getId().toString())
                                .build()))
                )
                .onItem().ifNull().continueWith(CreateOrUpdateUserByFiscalCodeResponse.builder()
                        .operationType(OPERATION_TYPE.SKIPPED)
                        .userId(userResource.getId().toString())
                        .build())
                .onFailure().invoke(exception -> log.error("Error during persist user on UserInstitution: {} ", exception.getMessage(), exception));
    }

    /**
     * The createUserOnUserRegistryAndUserInstitution method is a method that creates a user on both the user registry and the user institution.
     * The method starts by generates a new mailUuid and creates a workContacts map. It adds a new work contact to the map using the mailUuid
     * as the key and the institution email from the userDto object as the value.
     * Next, it attempts to save the user on the user registry using a PATCH request.
     * After the user registry save operation, the method attempts to find the user institution by the user ID and institution ID from the userDto object.
     * It then updates or creates the userinstitution model and it attempts to persist the user institution.
     * If any of the operations fail, the method logs an error message and returns a Uni that emits a failure.
     */
    private Uni<CreateOrUpdateUserByFiscalCodeResponse> createUserOnUserRegistryAndUserInstitution(CreateUserDto userDto,LoggedUser loggedUser) {
        log.info("Creating user on userRegistry and userInstitution");
        String mailUuid = randomMailId.get();
        Map<String, WorkContactResource> workContacts = new HashMap<>();
        workContacts.put(mailUuid, UserUtils.buildWorkContact(userDto.getUser().getInstitutionEmail()));
        return userRegistryService.saveUsingPATCH(userMapper.toSaveUserDto(userDto.getUser(), workContacts))
            .onFailure(UserUtils::isConflictOnUserRegistry).retry().withBackOff(Duration.ofSeconds(2)).atMost(3)
            .onFailure().invoke(exception -> log.error("Error during create user on userRegistry: {} ", exception.getMessage(), exception))
            .onItem().invoke(userResource -> log.info("User created with id {}", userResource.getId()))
            .onItem().transform(userResource -> userResource.getId().toString())
            .onItem().transformToUni(userId -> updateOrCreateUserInstitution(userDto, mailUuid, null, userId)
                .onItem().ifNotNull().transformToUni(userInstitution -> userInstitutionService.persistOrUpdate(userInstitution)
                    .onItem().invoke(() -> log.info("UserInstitution persisted with userId:{}, institutionId:{}", userInstitution.getUserId(), userInstitution.getInstitutionId()))
                    .onItem().transformToUni(userInstitutionPersisted -> userRegistryService.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitutionPersisted.getUserId())
                        .onItem().transformToUni(userResourceUpdated -> sendNotificationsAndReturnData(userInstitution, userResourceUpdated, userDto.getProduct().getProductRoles(), userDto.getHasToSendEmail(), loggedUser, userDto.getProduct().getProductId(), QueueEvent.ADD))
                        .map(ignore -> CreateOrUpdateUserByFiscalCodeResponse.builder()
                                .operationType(OPERATION_TYPE.CREATED_OR_UPDATED)
                                .userId(userId)
                                .build())
                    )
                .onItem().ifNull().continueWith(CreateOrUpdateUserByFiscalCodeResponse.builder()
                        .operationType(OPERATION_TYPE.SKIPPED)
                        .userId(userId)
                        .build())))
            .onFailure().invoke(exception -> log.error("Error during persist user on UserInstitution: {} ", exception.getMessage(), exception));
    }

    /**
     * The createOrUpdateUserByUserId method is a method that either add to existingUser a new user Role.
     * The method starts by calling the findByIdUsingGET method on the userRegistryService object,
     * If the search operation fails with a UserNotFoundException, it recovers by returning a Uni that fails with a ResourceNotFoundException.
     * If the search operation is successful, it call method to Update user on UserInstitution collection.
     * Finally, if any operation fails, it logs an error message and returns a Uni that emits a failure.
     */
    @Override
    public Uni<String> createOrUpdateUserByUserId(AddUserRoleDto userDto, String userId, LoggedUser loggedUser, OnboardedProductState status) {
        return userRegistryService.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId)
                .onFailure(UserUtils::isUserNotFoundExceptionOnUserRegistry).retry().atMost(2)
                .onFailure(UserUtils::isUserNotFoundExceptionOnUserRegistry).recoverWithUni(throwable -> Uni.createFrom().failure(new ResourceNotFoundException(throwable.getMessage())))
                .onItem().transformToUni(userResource -> updateUserInstitutionByUserId(userResource, userDto, loggedUser, status))
                .onFailure().invoke(exception -> log.error("Error during retrieve user from userRegistry: {} ", exception.getMessage(), exception));
    }

    public Uni<String> createUserByUserId(AddUserRoleDto userDto, String userId, LoggedUser loggedUser) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(userDto.getInstitutionId()).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(userDto.getProduct().getProductId()).status(List.of(ACTIVE, SUSPENDED)).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().transformToUni(userInstitution -> {
                    if (Optional.ofNullable(userInstitution).isPresent()) {
                        log.info("User with userId: {} has already onboarded for product {}. Proceeding with check role", Encode.forJava(userId), Encode.forJava(userDto.getProduct().getProductId()));
                        PartyRole roleOnProduct = retrieveUserRoleOnProduct(userInstitution, userDto.getProduct().getProductId());
                        OnboardedProductState currentStatus = retrieveUserStatusOnProduct(userInstitution, userDto.getProduct().getProductId());
                        return checkAndUpdateUserMail(userInstitution, userDto.getUserMailUuid())
                                .onItem().transformToUni(ignore -> evaluateRoleAndCreateOrUpdateUserByUserId(userDto, userId, loggedUser, roleOnProduct, currentStatus));
                    } else {
                        log.info("User with userId: {} has not onboarded for product {}. Proceeding with create", Encode.forJava(userId), Encode.forJava(userDto.getProduct().getProductId()));
                        return createOrUpdateUserByUserId(userDto, userId, loggedUser, ACTIVE);
                    }
                })
                .onFailure().invoke(exception -> log.error("Error during createOrUpdateManagerByUserId for userId: {}, institutionId: {}: {}", Encode.forJava(userId), Encode.forJava(userDto.getInstitutionId()), exception.getMessage(), exception));
    }

    /**
     * Check if the user's mail uuid is still the same on UserInstitution and update it if not
     */
    private Uni<UserInstitution> checkAndUpdateUserMail(UserInstitution userInstitution, String userMailUuid) {
        if (userMailUuid == null || userMailUuid.equals(userInstitution.getUserMailUuid())) {
            log.info("UserMailUuid not changed for user {} and institution {}", userInstitution.getUserId(), userInstitution.getInstitutionId());
            return Uni.createFrom().item(userInstitution);
        }
        userInstitution.setUserMailUuid(userMailUuid);
        userInstitution.setUserMailUpdatedAt(OffsetDateTime.now());
        log.info("Updating userMailUuid for user {} and institution {}", userInstitution.getUserId(), userInstitution.getInstitutionId());
        return userInstitutionService.persistOrUpdate(userInstitution);
    }

    /**
     * The evaluateRoleAndCreateOrUpdateUserByUserId method is designed to evaluate the role of a user for a specific product
     * and either create or update the user's role based on certain conditions.
     * First, the method checks if the roleOnProduct is null. If it is, we need to create new role for the user on selected product.
     * Next, if the new role is MANAGER:
     * - If the user already has the MANAGER role, no new role is created and the method completes successfully.
     * - If the user has a different role, the existing role is marked as DELETED and a new MANAGER role is created
     *   with the same status as the previous one.
     * If the new role is not MANAGER and the user already has any role on the product, the method throws a
     * UserRoleAlreadyPresentException to prevent assigning a conflicting role.
     */
    private Uni<String> evaluateRoleAndCreateOrUpdateUserByUserId(AddUserRoleDto userDto, String userId, LoggedUser loggedUser, PartyRole roleOnProduct, OnboardedProductState currentStatus) {
        PartyRole newRole;
        try {
            newRole = PartyRole.valueOf(userDto.getProduct().getRole());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid role: " + userDto.getProduct().getRole() + ". Allowed value are: " + Arrays.toString(PartyRole.values()));
        }
        if (Objects.isNull(roleOnProduct)) {
            return createOrUpdateUserByUserId(userDto, userId, loggedUser, ACTIVE);
        }

        if (PartyRole.MANAGER.equals(newRole)) {
            if (PartyRole.MANAGER.equals(roleOnProduct)) {
                log.info("User {} already has MANAGER role with status {}. No changes needed.", Encode.forJava(userId), currentStatus);
                return Uni.createFrom().failure(new UserRoleAlreadyPresentException(
                        String.format("User already has MANAGER role with status %s for product [%s].", currentStatus, userDto.getProduct().getProductId())));
            } else {
                log.info("User {} has role {} with status {}. Replacing with MANAGER role keeping same status.", Encode.forJava(userId), roleOnProduct, currentStatus);
                return userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(
                                userId, userDto.getInstitutionId(), userDto.getProduct().getProductId(), null, null, DELETED)
                        .onItem().transformToUni(ignore -> createOrUpdateUserByUserId(userDto, userId, loggedUser, currentStatus));
            }
        } else {
            log.info("User {} already has status {} for product {}. Cannot assign {} role.", Encode.forJava(userId), Encode.forJava(String.valueOf(currentStatus)), Encode.forJava(userDto.getProduct().getProductId()), Encode.forJava(String.valueOf(newRole)));
            return Uni.createFrom().failure(new UserRoleAlreadyPresentException(
                    String.format("User already has status %s for product [%s]. Cannot assign %s role.",
                            currentStatus, userDto.getProduct().getProductId(), newRole)));
        }
    }

    /**
     * This method retrieve the user role on a specific product if exists (status = ACTIVE).
     */
    private PartyRole retrieveUserRoleOnProduct(UserInstitution userInstitution, String productId) {
        if (Objects.nonNull(userInstitution.getProducts()) && !userInstitution.getProducts().isEmpty()) {
            return userInstitution.getProducts().stream()
                    .filter(onboardedProduct -> productId.equalsIgnoreCase(onboardedProduct.getProductId()))
                    .filter(onboardedProduct -> List.of(ACTIVE, SUSPENDED).contains(onboardedProduct.getStatus()))
                    .findFirst()
                    .map(OnboardedProduct::getRole)
                    .orElse(null);
        }
        return null;
    }

    private OnboardedProductState retrieveUserStatusOnProduct(UserInstitution userInstitution, String productId) {
        if (Objects.nonNull(userInstitution.getProducts()) && !userInstitution.getProducts().isEmpty()) {
            return userInstitution.getProducts().stream()
                    .filter(onboardedProduct -> productId.equalsIgnoreCase(onboardedProduct.getProductId()))
                    .filter(onboardedProduct -> List.of(ACTIVE, SUSPENDED).contains(onboardedProduct.getStatus()))
                    .findFirst()
                    .map(OnboardedProduct::getStatus)
                    .orElse(null);
        }
        return null;
    }


    /**
     * Updates or creates a UserInstitution by userId and institutionId, persists the changes,
     * and sends notifications if needed. If the productRole already exists return nullItem.
     *
     * @param userResource The resource representing the user.
     * @param userDto The DTO containing user role and institution information.
     * @param loggedUser The currently logged-in user.
     * @return A Uni containing the userId as a string.
     */
    private Uni<String> updateUserInstitutionByUserId(UserResource userResource, AddUserRoleDto userDto, LoggedUser loggedUser, OnboardedProductState status) {
        return userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), userDto.getInstitutionId())
                .onItem().transformToUni(userInstitution -> updateOrCreateUserInstitution(userDto, userInstitution, userResource.getId().toString(), status))
                .onItem().ifNotNull().transformToUni(userInstitutionService::persistOrUpdate)
                .onItem().ifNotNull().invoke(userInstitution -> log.info("UserInstitution with userId: {}, institutionId: {} persisted", userInstitution.getUserId(), userInstitution.getInstitutionId()))
                .onFailure().invoke(exception -> log.error("Error during persist user on UserInstitution: {} ", exception.getMessage(), exception))
                .onItem().ifNotNull().transformToUni(userInstitution -> sendNotificationsAndReturnData(userInstitution, userResource, userDto.getProduct().getProductRoles(), userDto.isHasToSendEmail(), loggedUser, userDto.getProduct().getProductId(), QueueEvent.UPDATE)
                    .replaceWith(userResource.getId().toString()));

    }

    private Uni<UserInstitution> updateOrCreateUserInstitution(AddUserRoleDto userDto, UserInstitution userInstitution, String userId, OnboardedProductState status) {
        if (userInstitution == null) {
            log.info(USER_INSTITUTION_NOT_FOUND, userId, userDto.getInstitutionId());
            return Uni.createFrom().item(userInstitutionMapper.toNewEntity(userDto, userId));
        }

        log.info(USER_INSTITUTION_FOUNDED, userId, userDto.getInstitutionId());

        if(checkAlreadyOnboardedRole(userDto.getProduct(), userInstitution)){
            throw new InvalidRequestException(String.format("User already has different role on Product %s", userDto.getProduct().getProductId()));
        }

        List<String> productRoleToAdd = checkAlreadyOnboardedProductRole(userDto.getProduct().getProductId(), userDto.getProduct().getProductRoles(), userInstitution);
        userDto.getProduct().setProductRoles(productRoleToAdd);

        productRoleToAdd.forEach(productRole -> userInstitution.getProducts().add(onboardedProductMapper.toNewOnboardedProduct(userDto.getProduct(), productRole, status)));
        return Uni.createFrom().item(userInstitution);
    }

    private boolean checkAlreadyOnboardedRole(AddUserRoleDto.Product product, UserInstitution userInstitution) {
        return Optional.ofNullable(userInstitution.getProducts())
                .orElse(Collections.emptyList())
                .stream()
                .filter(onboardedProduct -> List.of(ACTIVE, SUSPENDED).contains(onboardedProduct.getStatus()))
                .filter(onboardedProduct -> onboardedProduct.getProductId().equalsIgnoreCase(product.getProductId()))
                .anyMatch(onboardedProduct -> !onboardedProduct.getRole().name().equalsIgnoreCase(product.getRole()));
    }

    private boolean checkAlreadyOnboardedRole(CreateUserDto.Product product, UserInstitution userInstitution) {
        return Optional.ofNullable(userInstitution.getProducts())
                .orElse(Collections.emptyList())
                .stream()
                .filter(onboardedProduct -> List.of(ACTIVE, SUSPENDED).contains(onboardedProduct.getStatus()))
                .filter(onboardedProduct -> onboardedProduct.getProductId().equalsIgnoreCase(product.getProductId()))
                .anyMatch(onboardedProduct -> !onboardedProduct.getRole().name().equalsIgnoreCase(product.getRole()));
    }

    /**
     * Updates or creates a UserInstitution based on the provided data.
     * Return null value if UserInstitution exists in ACTIVE with the same productRole
     *
     * @param userDto DTO containing user data including institution and product roles.
     * @param mailUuid Unique identifier for the user's email.
     * @param userInstitution Existing UserInstitution entity or null if it doesn't exist.
     * @param userId Unique identifier for the user.
     * @return Uni<UserInstitution> Reactive type representing the UserInstitution entity after update or creation.
     */
    private Uni<UserInstitution> updateOrCreateUserInstitution(CreateUserDto userDto, String mailUuid, UserInstitution userInstitution, String userId) {
        if (userInstitution == null) {
            log.info(USER_INSTITUTION_NOT_FOUND, userId, userDto.getInstitutionId());
            return Uni.createFrom().item(userInstitutionMapper.toNewEntity(userDto, userId, mailUuid));
        }

        log.info(USER_INSTITUTION_FOUNDED, userId, userDto.getInstitutionId());
        log.info(USER_INSTITUTION_FOUNDED, userId, userDto.getInstitutionId());

        if(checkAlreadyOnboardedRole(userDto.getProduct(), userInstitution)){
            throw new InvalidRequestException(String.format("User already has different role on Product %s", userDto.getProduct().getProductId()));
        }

        List<String> productRoleToAdd = checkAlreadyOnboardedProductRole(userDto.getProduct().getProductId(), userDto.getProduct().getProductRoles(), userInstitution);
        userDto.getProduct().setProductRoles(productRoleToAdd);

        userInstitution.setUserMailUuid(mailUuid);
        productRoleToAdd.forEach(productRole -> userInstitution.getProducts().add(onboardedProductMapper.toNewOnboardedProduct(userDto.getProduct(), productRole)));

        return Uni.createFrom().item(userInstitution);
    }

    private List<String> checkAlreadyOnboardedProductRole(String productId, List<String> productRole, UserInstitution userInstitution) {

        List<String> productAlreadyOnboarded = new ArrayList<>(Optional.ofNullable(userInstitution.getProducts())
                .orElse(Collections.emptyList())
                .stream()
                .filter(onboardedProduct -> onboardedProduct.getProductId().equals(productId))
                .filter(onboardedProduct -> productRole.contains(onboardedProduct.getProductRole()))
                .filter(onboardedProduct -> List.of(ACTIVE, SUSPENDED).contains(onboardedProduct.getStatus()))
                .map(OnboardedProduct::getProductRole)
                .toList());

        List<String> productRoleFinal = new ArrayList<>(productRole);
        productRoleFinal.removeIf(productAlreadyOnboarded::contains);

        if (!productAlreadyOnboarded.isEmpty() && CollectionUtils.isNullOrEmpty(productRoleFinal)) {
            throw new InvalidRequestException(String.format("User already has roles on Product %s", productId));
        }
        return productRoleFinal;
    }


    /**
     * The retrieveUsers function is used to retrieve a list of users from the database and userRegistry.
     * The function takes in an userId, institutionId, personId, roles, states, products and productRoles as parameters.
     * It then calls the retrieveAdminUserInstitution function with these parameters which retrieves an UserInstitution document associated with an logged user (admin)
     * If this userInstitution object is not null it transform the item into either the personId or if this is null into just the uuid of that particular user (userUuid).
     * After this, the retrieveFilteredUserInstitutions method retrieves user institutions based on a variety of filters, including the previously retrieved
     * user ID, institution ID, roles, states, products, and product roles.
     * The function then retrieves the users by ID from userRegistry and maps for each element the userInstitution and userResource to a UserDataResponse object.
     */
    @Override
    public Multi<UserDataResponse> retrieveUsersData(String institutionId, String personId, List<String> roles, List<String> states, List<String> products, List<String> productRoles, String userUuid) {
        return retrieveAdminUserInstitution(institutionId, userUuid)
                .onItem().ifNotNull().invoke(userInstitution -> log.info("admin userInstitution found: {}", userInstitution))
                .onItem().transform(userInstitution -> userInstitution == null ? userUuid : personId)
                .onItem().invoke(userId -> log.info("userId to retrieve: {}", userId))
                .onItem().transformToMulti(user -> retrieveFilteredUserInstitutions(user, institutionId, roles, states, products, productRoles))
                .onItem().transform(userInstitution -> applyFiltersToRemoveProducts(userInstitution, states, products, roles, productRoles))
                .onItem().invoke(userInstitution -> log.info("userInstitution found: {}", userInstitution))
                .onItem().transformToUniAndMerge(userInstitution ->
                        userRegistryService.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                                .map(userResource -> userMapper.toUserDataResponse(userInstitution, userResource))
                );
    }

    @Override
    public Uni<Void> updateInstitutionDescription(String institutionId, UpdateDescriptionDto updateDescriptionDto) {
        return userInstitutionService.updateInstitutionDescription(institutionId, updateDescriptionDto)
                .onFailure().invoke(exception -> log.error("Error during update institution description with id {} on UserInstitution: {} ",
                        Encode.forJava(institutionId) , exception.getMessage(), exception))
                .replaceWithVoid();
    }

    /**
     * Retrieves a list of filtered user institutions by date and page count,
     * and sends events concurrently for each page.
     *
     * @param userId The ID of the user for whom to retrieve institutions.
     * @param institutionId The ID of the institution.
     * @param fromDate The starting date from which to filter the institutions.
     * @return A Uni representing the result of sending events for each page.
     */
    @Override
    public Uni<Void> sendEventsByDateAndUserIdAndInstitutionId(OffsetDateTime fromDate, String institutionId, String userId) {

        return retrieveFilteredUserInstitutionsByDatePageCount(userId,institutionId,fromDate)
                .onItem().transformToUni(pageCount -> pageCount > 0
                        ? Uni.combine().all().unis(
                                IntStream.range(0, pageCount).boxed()
                                .map(index -> sendEventsByDateAndUserIdAndInstitutionId(fromDate,institutionId,userId,index)
                                        .onItem().delayIt().by(Duration.ofSeconds(5)))
                                .toList())
                            .usingConcurrencyOf(eventhubUsersConcurrencyLevel)
                            .discardItems()
                        : Uni.createFrom().voidItem());
    }


    public Uni<Void> sendEventsByDateAndUserIdAndInstitutionId(OffsetDateTime fromDate, String institutionId, String userId, Integer page) {
        Multi<UserInstitution> userInstitutions = retrieveFilteredUserInstitutionsByDate(userId, institutionId, fromDate, page);
        return userInstitutions
                .onItem().transformToUni(userInstitution -> {
                    String userIdToUse = userId != null ? userId : userInstitution.getUserId();
                    Uni<UserResource> userResourceUni = userRegistryService.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userIdToUse);
                    TrackEventInput trackEventInput = TrackEventInput.builder()
                            .documentKey(userInstitution.getId().toHexString())
                            .userId(userIdToUse)
                            .institutionId(userInstitution.getInstitutionId())
                            .build();

                    return userResourceUni
                            .onItem().transformToUni(userResource -> buildAndSendKafkaNotifications(userInstitution, userResource)
                                    .collect().asList()
                                    .replaceWithVoid())
                            .onItem().invoke(() -> trackTelemetryEvent(trackEventInput, EVENTS_USER_INSTITUTION_SUCCESS))
                            .onFailure().invoke(exception -> log.error("Failed to retrieve UserResource userId:{}", userIdToUse, exception))
                            .onFailure().invoke(exception -> trackTelemetryEvent(trackEventInput.toBuilder().exception(exception.getMessage()).build(), EVENTS_USER_INSTITUTION_FAILURE))
                            .onFailure().recoverWithNull();
                })
                .merge().toUni()
                .onFailure().invoke(exception -> log.error("Failed to send Events for page: {}, message: {}", page, exception.getMessage()));
    }

    private void trackTelemetryEvent(TrackEventInput trackEventInput, String metricsName) {
        telemetryClient.trackEvent(EVENT_USER_MS_NAME, mapPropsForTrackEvent(trackEventInput), Map.of(metricsName, 1D));
    }

    private UserInstitution applyFiltersToRemoveProducts(UserInstitution userInstitution, List<String> states, List<String> products, List<String> roles, List<String> productRoles) {
        if(!CollectionUtils.isNullOrEmpty(userInstitution.getProducts())) {
            userInstitution.getProducts().removeIf(product ->
                    (!CollectionUtils.isNullOrEmpty(states) && !states.contains(product.getStatus().name())) ||
                    (!CollectionUtils.isNullOrEmpty(products) && !products.contains(product.getProductId())) ||
                    (!CollectionUtils.isNullOrEmpty(roles) && !roles.contains(product.getRole().name())) ||
                    (!CollectionUtils.isNullOrEmpty(productRoles) && !productRoles.contains(product.getProductRole()))
            );
        }
        return userInstitution;
    }

    private Multi<UserInstitution> retrieveFilteredUserInstitutions(String user, String institutionId, List<String> roles, List<String> states, List<String> products, List<String> productRoles) {
        var institutionFilters = UserInstitutionFilter.builder().userId(user).institutionId(institutionId).build().constructMap();
        var prodFilter = OnboardedProductFilter.builder().role(roles).status(states).productRole(productRoles).productId(products).build().constructMap();
        var queryParam = userUtils.retrieveMapForFilter(institutionFilters, prodFilter);
        return userInstitutionService.findAllWithFilter(queryParam);
    }

    private Multi<UserInstitution> retrieveFilteredUserInstitutionsByDate(String userId, String institutionId, OffsetDateTime fromDate){
        var queryParam = retrieveFilteredUserInstitutionsByDateQueryParam(userId,institutionId,fromDate);
        return userInstitutionService.findUserInstitutionsAfterDateWithFilter(queryParam, fromDate);
    }

    private Multi<UserInstitution> retrieveFilteredUserInstitutionsByDate(String userId, String institutionId, OffsetDateTime fromDate, Integer page){
        var queryParam = retrieveFilteredUserInstitutionsByDateQueryParam(userId,institutionId,fromDate);
        return userInstitutionService.findUserInstitutionsAfterDateWithFilter(queryParam, fromDate, page);
    }

    private Uni<Integer> retrieveFilteredUserInstitutionsByDatePageCount(String userId, String institutionId, OffsetDateTime fromDate){
        var queryParam = retrieveFilteredUserInstitutionsByDateQueryParam(userId,institutionId,fromDate);
        return userInstitutionService.pageCountUserInstitutionsAfterDateWithFilter(queryParam, fromDate);
    }

    private Map<String, Object> retrieveFilteredUserInstitutionsByDateQueryParam(String userId, String institutionId, OffsetDateTime fromDate) {
        var institutionFilters = UserInstitutionFilter.builder().institutionId(institutionId).userId(userId).build().constructMap();
        var productFilter = OnboardedProductFilter.builder().build().constructMap();
        return userUtils.retrieveMapForFilter(institutionFilters, productFilter);
    }

    private Uni<UserInstitution> retrieveAdminUserInstitution(String institutionId, String userUuid) {
        List<String> adminPartyRole = Arrays.stream(PartyRole.values()).filter(role -> role != PartyRole.OPERATOR).map(Enum::name).toList();
        List<String> validStates = Stream.of(ACTIVE, PENDING).map(Enum::name).toList();
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userUuid).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().role(adminPartyRole).status(validStates).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter);
    }

    private Uni<String> sendNotificationsAndReturnData(UserInstitution userInstitution, UserResource userResource, List<String> roleLabels, Boolean hasToSendEmail, LoggedUser loggedUser, String productId, QueueEvent queueEvent) {
        if (hasToSendEmail) {
            PrepareNotificationData.PrepareNotificationDataBuilder notificationDataBuilder = PrepareNotificationData.builder()
                    .userResource(userResource)
                    .userInstitution(userInstitution)
                    .queueEvent(queueEvent);
            return retrieveProductAndAddToPrepareNotificationData(notificationDataBuilder, productId)
                    .map(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                            .onItem().transformToUni(notificationData -> userNotificationService.sendCreateUserNotification(userInstitution.getInstitutionDescription(),
                                    roleLabels, userResource, userInstitution, notificationData.getProduct(), loggedUser))
                    .replaceWith(userResource.getId().toString());
        } else {
            return Uni.createFrom().item(userResource.getId().toString());
        }
    }

    private Multi<UserNotificationToSend> buildAndSendKafkaNotifications(UserInstitution userInstitution, UserResource userResource){
        return Multi.createFrom().iterable(userUtils.buildUsersNotificationResponse(userInstitution, userResource))
                .onItem().transformToUniAndMerge(userNotificationService::sendKafkaNotification);
    }


    @Override
    public Uni<UserInstitutionWithActions> getUserInstitutionWithPermission(String userId, String institutionId, String productId) {
        String mappedProductId;
        Map<String, Object> queryParameter;
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        if (StringUtils.isNotEmpty(productId)) {
            mappedProductId = Optional.ofNullable(productService.getProduct(productId))
                    .map(Product::getParentId)
                    .orElse(productId);
            var productFilters = OnboardedProductFilter.builder().productId(mappedProductId).status(ACTIVE).build().constructMap();
            queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        } else {
            mappedProductId = null;
            queryParameter = userInstitutionFilters;
        }
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(new ResourceNotFoundException(String.format(USER_INSTITUTION_NOT_FOUND_ERROR.getMessage(), userId, institutionId), USER_INSTITUTION_NOT_FOUND_ERROR.getCode()))
                .onItem().transformToUni(item ->  mapToUserInstitutionPermission(item, mappedProductId))
                .onFailure().invoke(() -> log.error(String.format(USER_INSTITUTION_NOT_FOUND_ERROR.getMessage(), userId, institutionId)));
    }

    private Uni<UserInstitutionWithActions> mapToUserInstitutionPermission(UserInstitution userInstitution, String productId) {
        return Uni.createFrom().item(userInstitutionMapper.toUserInstitutionPermission(userInstitution))
                .onItem().invoke(userInstitutionWithPermission ->
                        userInstitutionWithPermission.setProducts(filterProductAndAddActions(userInstitutionWithPermission, productId)));
    }

    private List<OnboardedProductWithActions> filterProductAndAddActions(UserInstitutionWithActions userInstitutionWithActions, String productId) {
        return userInstitutionWithActions.getProducts().stream()
                .filter(onboardedProductWithActions -> ACTIVE.equals(onboardedProductWithActions.getStatus()))
                .filter(onboardedProductWithActions -> Objects.isNull(productId) || productId.equalsIgnoreCase(onboardedProductWithActions.getProductId()))
                .filter(onboardedProductWithActions -> StringUtils.isNotBlank(onboardedProductWithActions.getRole()))
                .peek(onboardedProductWithActions -> {
                    Map<String, List<String>> roleActions = actionMapRetriever.getUserActionsMap().get(onboardedProductWithActions.getRole());
                        List<String> productActions = roleActions.getOrDefault(onboardedProductWithActions.getProductId(), roleActions.get("default"));
                        onboardedProductWithActions.setUserProductActions(productActions);
                })
                .toList();
    }

    @Override
    public Uni<UsersCountResponse> getUsersCount(String institutionId, String productId, List<PartyRole> roles, List<OnboardedProductState> status) {
        final List<PartyRole> roleList = Optional.ofNullable(roles).filter(l -> !l.isEmpty()).orElse(List.of(PartyRole.values()));
        final List<OnboardedProductState> statusList = Optional.ofNullable(status).filter(l -> !l.isEmpty()).orElse(List.of(ACTIVE));
        return userInstitutionService.countUsers(institutionId, productId, roleList, statusList)
                .onItem().transform(count -> new UsersCountResponse(institutionId, productId, roleList, statusList, count));
    }

    @Override
    public Uni<Boolean> checkUser(String fiscalCode, String institutionId, String productId) {
        return userRegistryService.searchUsingPOST(USER_FIELD_LIST_WITHOUT_WORK_CONTACTS, new UserSearchDto(fiscalCode))
                .flatMap(user -> userInstitutionService.existsValidUserProduct(
                        user.getId().toString(),
                        institutionId,
                        productId,
                        PermissionTypeEnum.ANY,
                        List.of(ACTIVE,SUSPENDED)
                ))
                .onFailure(this::isNotFound)
                .recoverWithItem(false);
    }

    @Override
    public Uni<Void> sendEmailOtp(String userId, String institutionalEmail, String otp) {
        return userRegistryService.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userId)
                .map(userResource -> userResource.getName().getValue()).chain(name ->
                        userNotificationService.sendOtpNotification(institutionalEmail,name, otp)
                );
    }

    private boolean isNotFound(Throwable throwable) {
        return ((WebApplicationException) throwable).getResponse().getStatus() == 404;
    }

}
