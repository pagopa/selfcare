package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.UserDataResponse;
import it.pagopa.selfcare.user.controller.response.UserDetailResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.OnboardedProductMapper;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.notification.PrepareNotificationData;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static it.pagopa.selfcare.user.constant.CollectionUtil.MAIL_ID_PREFIX;
import static it.pagopa.selfcare.user.constant.CustomError.*;
import static it.pagopa.selfcare.user.constant.OnboardedProductState.ACTIVE;
import static it.pagopa.selfcare.user.constant.OnboardedProductState.PENDING;
import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;
import static it.pagopa.selfcare.user.util.UserUtils.VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @RestClient
    @Inject
    private UserApi userRegistryApi;

    private final UserMapper userMapper;
    private final UserInstitutionMapper userInstitutionMapper;
    private final OnboardedProductMapper onboardedProductMapper;

    private final UserUtils userUtils;

    private final ProductService productService;
    private final UserInstitutionService userInstitutionService;
    private final UserNotificationService userNotificationService;

    Supplier<String> randomMailId = () -> (MAIL_ID_PREFIX + UUID.randomUUID());

    private static final String WORK_CONTACTS = "workContacts";

    private static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,email,name,workContacts";

    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";

    /**
     * The updateUserStatus function updates the status of a user's onboarded product.
     */
    @Override
    public Uni<Void> updateUserStatusWithOptionalFilter(String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status) {

        if (status == null) {
            return Uni.createFrom().failure(new InvalidRequestException(STATUS_IS_MANDATORY.getMessage()));
        }

        userUtils.checkProductRole(productId, role, productRole);

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
        var productFilters = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Multi<UserInstitution> userInstitutions = userInstitutionService.findAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters));
        return userInstitutions.onItem()
                .transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(WORK_CONTACTS, userInstitution.getUserId())
                        .map(userResource -> Objects.nonNull(userResource.getWorkContacts()) && userResource.getWorkContacts().containsKey(userInstitution.getUserMailUuid())
                                ? userResource.getWorkContacts().get(userInstitution.getUserMailUuid()) : null)).merge()
                .filter(workContactResource -> Objects.nonNull(workContactResource) && StringUtils.isNotBlank(workContactResource.getEmail().getValue()))
                .map(workContactResource -> workContactResource.getEmail().getValue())
                .collect().asList();

    }

    @Override
    public Multi<UserProductResponse> getUserProductsByInstitution(String institutionId) {
        Multi<UserInstitution> userInstitutions = UserInstitution.find(UserInstitution.Fields.institutionId.name(), institutionId).stream();
        return userInstitutions.onItem()
                .transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                        .map(userResource -> UserProductResponse.builder()
                                .id(userResource.getId().toString())
                                .name(userResource.getName().getValue())
                                .surname(userResource.getFamilyName().getValue())
                                .taxCode(userResource.getFiscalCode())
                                .products(onboardedProductMapper.toList(userInstitution.getProducts()))
                                .build()))
                .merge();
    }

    @Override
    public Uni<UserResource> retrievePerson(String userId, String productId, String institutionId) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(() -> {
                    log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId));
                    return new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode());
                })
                .onItem().transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId()))
                .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode()));
    }

    @Override
    public Multi<UserInstitutionResponse> findAllUserInstitutions(String institutionId, String userId, List<String> roles, List<String> states, List<String> products, List<String> productRoles) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(products).status(states).role(roles).productRole(productRoles).build().constructMap();
        return userInstitutionService.findAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters))
                .onItem().transform(userInstitutionMapper::toResponse);
    }

    @Override
    public Multi<UserInstitutionResponse> findPaginatedUserInstitutions(String institutionId, String userId, List<PartyRole> roles, List<String> states, List<String> products, List<String> productRoles, Integer page, Integer size) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(products).status(states).role(roles).productRole(productRoles).build().constructMap();
        return userInstitutionService.paginatedFindAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters), page, size)
                .onItem().transform(userInstitutionMapper::toResponse);
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
    public Uni<List<UserInstitutionResponse>> findAllByIds(List<String> userIds) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(formatQueryParameterList(userIds)).build().constructMap();
        return userInstitutionService.findAllWithFilter(userUtils.retrieveMapForFilter(userInstitutionFilters))
                .collect()
                .asList().onItem().transform(userInstitutions -> userInstitutions.stream().map(userInstitutionMapper::toResponse).toList());
    }

    @Override
    public Uni<Void> updateUserProductCreatedAt(String institutionId, List<String> userIds, String productId, LocalDateTime createdAt) {
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
                .onItem().transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(fields, userId)
                        .map(userResource -> userMapper.toUserDetailResponse(userResource, Optional.ofNullable(institutionId).map(ignored -> userInstitution.getUserMailUuid()).orElse(null))))
                .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode()));
    }

    @Override
    public Uni<UserDetailResponse> searchUserByFiscalCode(String fiscalCode, String institutionId) {
        Uni<UserResource> userResourceUni = userRegistryApi.searchUsingPOST(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, new UserSearchDto(fiscalCode))
                .onFailure(UserUtils::checkIfNotFoundException).transform(t -> new ResourceNotFoundException("User not found", USER_NOT_FOUND_ERROR.getCode()));

        return userResourceUni
                .onItem()
                .transformToUni(userResource -> {
                    var userInstitutionFilters = UserInstitutionFilter.builder().userId(userResource.getId().toString()).institutionId(institutionId).build().constructMap();
                    Uni<String> userMailUuid = userInstitutionService.retrieveFirstFilteredUserInstitution(userUtils.retrieveMapForFilter(userInstitutionFilters))
                            .onItem().ifNull().continueWith(() -> {
                                log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userResource.getId()));
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
    public Uni<Void> updateUserProductStatus(String userId, String institutionId, String productId, OnboardedProductState status, LoggedUser loggedUser) {
        PrepareNotificationData.PrepareNotificationDataBuilder prepareNotificationDataBuilder = PrepareNotificationData.builder();
        return updateUserStatusWithOptionalFilter(userId, institutionId, productId, null, null, status)
                .onItem().transformToUni(unused -> retrieveUserFromUserRegistryAndAddToPrepareNotificationData(prepareNotificationDataBuilder, userId))
                .onItem().transformToUni(builder -> retrieveUserInstitutionAndAddToPrepareNotificationData(builder, userId, institutionId))
                .onItem().transformToUni(builder -> retrieveProductAndAddToPrepareNotificationData(builder, productId))
                .onItem().transform(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                .onItem().transformToUni(prepareNotificationData -> userNotificationService.sendEmailNotification(prepareNotificationData.getUserResource(), prepareNotificationData.getUserInstitution(), prepareNotificationData.getProduct(), status, loggedUser.getName(), loggedUser.getFamilyName())
                        .onFailure().recoverWithNull()
                        .replaceWith(prepareNotificationData))
                .onItem().transform(prepareNotificationData -> userUtils.buildUserNotificationToSend(prepareNotificationData.getUserInstitution(), prepareNotificationData.getUserResource(), productId, status))
                .onItem().call(userNotificationToSend -> userNotificationService.sendKafkaNotification(userNotificationToSend, userId))
                .onFailure().invoke(throwable -> log.error("Error during update user status for userId: {}, institutionId: {}, productId:{} -> exception: {}", userId, institutionId, productId, throwable.getMessage(), throwable))
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

    private Uni<PrepareNotificationData.PrepareNotificationDataBuilder> retrieveUserFromUserRegistryAndAddToPrepareNotificationData(PrepareNotificationData.PrepareNotificationDataBuilder prepareNotificationDataBuilder, String userId) {
        return userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userId)
                .onItem().transform(prepareNotificationDataBuilder::userResource);
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
                .onItem().transformToUniAndMerge(userInstitution -> userRegistryApi
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
                                throw new ResourceNotFoundException("");
                            }
                            return filteredUserInfo;
                        })
                        .orElseThrow(() -> new ResourceNotFoundException(""))
                );
    }

    /**
     * The createOrUpdateUserByFiscalCode method is a method that either creates a new user or updates an existing one based on the provided CreateUserDto object.
     * The method starts by calling the searchUsingPOST method on the userRegistryApi object, this is an asynchronous operation that searches for a user in the user registry.
     * If the search operation fails with a UserNotFoundException, it recovers by returning a Uni that fails with a ResourceNotFoundException.
     * This is a way of transforming one type of exception into another.
     * If the search operation is successful, it call method to Update user on userRegistry and UserInstitution collection.
     * In case of previously fail with ResourceNotFoundException, it recovers by calling the method to create new User on userRegsitry and UserInstitution
     * collection.
     * In both cases at the end of persistence it sends an email and events on kafka
     * Finally, if any operation fails, it logs an error message and returns a Uni that emits a failure.
     */
    @Override
    public Uni<String> createOrUpdateUserByFiscalCode(CreateUserDto userDto, LoggedUser loggedUser) {
        return userRegistryApi.searchUsingPOST(USERS_WORKS_FIELD_LIST, new UserSearchDto(userDto.getUser().getFiscalCode()))
                .onFailure(UserUtils::isUserNotFoundExceptionOnUserRegistry).recoverWithUni(throwable -> Uni.createFrom().failure(new ResourceNotFoundException(throwable.getMessage())))
                .onItem().transformToUni(userResource -> updateUserOnUserRegistryAndUserInstitutionByFiscalCode(userResource, userDto))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> createUserOnUserRegistryAndUserInstitution(userDto))
                .onFailure().invoke(exception -> log.error("Error during retrieve user from userRegistry: {} ", exception.getMessage(), exception))
                .onItem().transformToUni(prepareNotificationData -> sendNotificationsAndReturnData(userDto.getInstitutionDescription(), userDto.getProduct().getProductRoles(), userDto.getHasToSendEmail(), loggedUser, prepareNotificationData))
                .onItem().transformToMulti(this::buildAndSendKafkaNotifications)
                .collect().first()
                .map(userNotificationSent -> userNotificationSent.getUser().getUserId());
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
    private Uni<PrepareNotificationData> updateUserOnUserRegistryAndUserInstitutionByFiscalCode(UserResource userResource, CreateUserDto userDto) {
        log.info("Updating user on userRegistry and userInstitution");
        String mailUuid = userUtils.getMailUuidFromMail(userResource.getWorkContacts(), userDto.getUser().getInstitutionEmail())
                .orElse(randomMailId.get());

        var workContact = UserUtils.buildWorkContact(userDto.getUser().getInstitutionEmail());
        userResource.getWorkContacts().put(mailUuid, workContact);

        return userRegistryApi.updateUsingPATCH(userResource.getId().toString(), userMapper.toMutableUserFieldsDto(userResource))
                .onFailure().invoke(exception -> log.error("Error during update user on userRegistry: {} ", exception.getMessage(), exception))
                .onItem().invoke(response -> log.info("User with id {} updated on userRegistry", userResource.getId()))
                .onItem().transformToUni(response -> userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), userDto.getInstitutionId()))
                .onItem().transform(userInstitution -> updateOrCreateUserInstitution(userDto, mailUuid, userInstitution, userResource.getId().toString()))
                .onItem().invoke(userInstitution -> log.info("start persist userInstititon: {}", userInstitution))
                .onItem().transformToUni(userInstitutionService::persistOrUpdate)
                .map(userInstitution -> PrepareNotificationData.builder().userResource(userResource).userInstitution(userInstitution).queueEvent(QueueEvent.UPDATE))
                .onItem().transformToUni(prepareNotificationDataBuilder -> retrieveProductAndAddToPrepareNotificationData(prepareNotificationDataBuilder, userDto.getProduct().getProductId()))
                .map(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                .onItem().invoke(() -> log.info("UserInstitution persisted"))
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
    private Uni<PrepareNotificationData> createUserOnUserRegistryAndUserInstitution(CreateUserDto userDto) {
        log.info("Creating user on userRegistry and userInstitution");
        String mailUuid = randomMailId.get();
        Map<String, WorkContactResource> workContacts = new HashMap<>();
        workContacts.put(mailUuid, UserUtils.buildWorkContact(userDto.getUser().getInstitutionEmail()));
        return userRegistryApi.saveUsingPATCH(userMapper.toSaveUserDto(userDto.getUser(), workContacts))
                .onFailure().invoke(exception -> log.error("Error during create user on userRegistry: {} ", exception.getMessage(), exception))
                .onItem().invoke(userResource -> log.info("User created with id {}", userResource.getId()))
                .onItem().transform(userResource -> userResource.getId().toString())
                .onItem().transform(userId -> updateOrCreateUserInstitution(userDto, mailUuid, null, userId))
                .onItem().invoke(userInstitution -> log.info("start persist userInstititon: {}", userInstitution))
                .onItem().transformToUni(userInstitutionService::persistOrUpdate)
                .onItem().transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                    .map(resource -> PrepareNotificationData.builder().userResource(resource).userInstitution(userInstitution).queueEvent(QueueEvent.ADD)))
                .onItem().transformToUni(prepareNotificationDataBuilder -> retrieveProductAndAddToPrepareNotificationData(prepareNotificationDataBuilder, userDto.getProduct().getProductId()))
                .map(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                .onItem().invoke(() -> log.info("UserInstitution persisted"))
                .onFailure().invoke(exception -> log.error("Error during persist user on UserInstitution: {} ", exception.getMessage(), exception));
    }

    private UserInstitution updateOrCreateUserInstitution(CreateUserDto userDto, String mailUuid, UserInstitution userInstitution, String userId) {
        if (userInstitution == null) {
            log.info("UserInstitution with userId: {} and institutionId: {} not found", userId, userDto.getInstitutionId());
            return userInstitutionMapper.toNewEntity(userDto, userId, mailUuid);
        }

        log.info("UserInstitution with userId: {} and institutionId: {} found", userId, userDto.getInstitutionId());
        userInstitution.setUserMailUuid(mailUuid);
        userInstitution.getProducts().add(onboardedProductMapper.toNewOnboardedProduct(userDto.getProduct()));

        return userInstitution;
    }

    /**
     * The createOrUpdateUserByUserId method is a method that either add to existingUser a new user Role.
     * The method starts by calling the findByIdUsingGET method on the userRegistryApi object,
     * If the search operation fails with a UserNotFoundException, it recovers by returning a Uni that fails with a ResourceNotFoundException.
     * If the search operation is successful, it call method to Update user on UserInstitution collection.
     * Finally, if any operation fails, it logs an error message and returns a Uni that emits a failure.
     */
    @Override
    public Uni<Void> createOrUpdateUserByUserId(AddUserRoleDto userDto, String userId, LoggedUser loggedUser) {
        return userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId)
                .onFailure(UserUtils::isUserNotFoundExceptionOnUserRegistry).recoverWithUni(throwable -> Uni.createFrom().failure(new ResourceNotFoundException(throwable.getMessage())))
                .onItem().transformToUni(userResource -> updateUserInstitutionByUserId(userResource, userDto, loggedUser))
                .onFailure().invoke(exception -> log.error("Error during retrieve user from userRegistry: {} ", exception.getMessage(), exception));
    }

    private Uni<Void> updateUserInstitutionByUserId(UserResource userResource, AddUserRoleDto userDto, LoggedUser loggedUser) {
        return userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), userDto.getInstitutionId())
                .onItem().transform(userInstitution -> updateOrCreateUserInstitution(userDto, userInstitution, userResource.getId().toString()))
                .onItem().invoke(userInstitution -> log.info("start persist userInstititon: {}", userInstitution))
                .onItem().transformToUni(userInstitutionService::persistOrUpdate)
                .onItem().invoke(() -> log.info("UserInstitution persisted"))
                .onFailure().invoke(exception -> log.error("Error during persist user on UserInstitution: {} ", exception.getMessage(), exception))
                .map(userInstitution -> PrepareNotificationData.builder().userResource(userResource).userInstitution(userInstitution).queueEvent(QueueEvent.UPDATE))
                .onItem().transformToUni(prepareNotificationDataBuilder -> retrieveProductAndAddToPrepareNotificationData(prepareNotificationDataBuilder, userDto.getProduct().getProductId()))
                .map(PrepareNotificationData.PrepareNotificationDataBuilder::build)
                .onItem().transformToUni(prepareNotificationData -> sendNotificationsAndReturnData(userDto.getInstitutionDescription(), userDto.getProduct().getProductRoles(), userDto.isHasToSendEmail(), loggedUser, prepareNotificationData))
                .onItem().transformToMulti(this::buildAndSendKafkaNotifications)
                .collect().first()
                .replaceWithVoid();

    }

    private UserInstitution updateOrCreateUserInstitution(AddUserRoleDto userDto, UserInstitution userInstitution, String userId) {
        if (userInstitution == null) {
            log.info("UserInstitution with userId: {} and institutionId: {} not found", userId, userDto.getInstitutionId());
            return userInstitutionMapper.toNewEntity(userDto, userId);
        }
        log.info("UserInstitution with userId: {} and institutionId: {} found", userId, userDto.getInstitutionId());
        userInstitution.getProducts().add(onboardedProductMapper.toNewOnboardedProduct(userDto.getProduct()));
        return userInstitution;
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
                .onItem().invoke(userInstitution -> removeProductsWithoutGivenState(userInstitution, states))
                .onItem().invoke(userInstitution -> log.info("userInstitution found: {}", userInstitution))
                .onItem().transformToUniAndMerge(userInstitution ->
                        userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                                .map(userResource -> userMapper.toUserDataResponse(userInstitution, userResource)));
    }

    private void removeProductsWithoutGivenState(UserInstitution userInstitution, List<String> states) {
        //remove product from list in userInstitution if product.getStatus is not contained in states
        userInstitution.getProducts().removeIf(product -> !states.contains(product.getStatus().name()));
    }

    private Multi<UserInstitution> retrieveFilteredUserInstitutions(String user, String institutionId, List<String> roles, List<String> states, List<String> products, List<String> productRoles) {
        var institutionFilters = UserInstitutionFilter.builder().userId(user).institutionId(institutionId).build().constructMap();
        var prodFilter = OnboardedProductFilter.builder().role(roles).status(states).productRole(productRoles).productId(products).build().constructMap();
        var queryParam = userUtils.retrieveMapForFilter(institutionFilters, prodFilter);
        return userInstitutionService.findAllWithFilter(queryParam);
    }

    private Uni<UserInstitution> retrieveAdminUserInstitution(String institutionId, String userUuid) {
        List<String> adminPartyRole = Arrays.stream(PartyRole.values()).filter(role -> role != PartyRole.OPERATOR).map(Enum::name).toList();
        List<String> validStates = Stream.of(ACTIVE, PENDING).map(Enum::name).toList();
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userUuid).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().role(adminPartyRole).status(validStates).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter);
    }

    private Uni<PrepareNotificationData> sendNotificationsAndReturnData(String institutionDescription, List<String> roleLabels, Boolean hasToSendEmail, LoggedUser loggedUser, PrepareNotificationData prepareNotificationData) {
        if (hasToSendEmail) {
            return userNotificationService.sendCreateUserNotification(institutionDescription, roleLabels, prepareNotificationData.getUserResource(), prepareNotificationData.getUserInstitution(), prepareNotificationData.getProduct(), loggedUser)
                    .replaceWith(prepareNotificationData);
        } else {
            return Uni.createFrom().item(prepareNotificationData);
        }
    }

    private Multi<UserNotificationToSend> buildAndSendKafkaNotifications(PrepareNotificationData prepareNotificationData) {
        return Multi.createFrom().iterable(userUtils.buildUsersNotificationResponse(prepareNotificationData.getUserInstitution(), prepareNotificationData.getUserResource(), prepareNotificationData.getQueueEvent()))
                .onItem().transformToUniAndMerge(notification -> userNotificationService.sendKafkaNotification(notification, notification.getUser().getUserId()));
    }
}
