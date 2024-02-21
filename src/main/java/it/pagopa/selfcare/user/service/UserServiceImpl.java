package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.OnboardedProductMapper;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.selfcare.user.constant.CustomError.*;
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
    private final OnboardedProductMapper onboardedProductMapper;
    private final UserUtils userUtils;
    private final UserInstitutionService userInstitutionService;
    private final UserInstitutionMapper userInstitutionMapper;

    private final UserNotificationService userNotificationService;
    private final ProductService productService;

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
    public Uni<UserResponse> retrievePerson(String userId, String productId, String institutionId) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(() -> {
                    log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId));
                    return new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode());
                })
                .onItem().transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                        .map(userResource -> userMapper.toUserResponse(userResource, userInstitution.getUserMailUuid())))
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
    public Uni<UserResource> getUserById(String userId) {
        return userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userId);
    }

    @Override
    public Uni<UserResource> searchUserByFiscalCode(String fiscalCode) {
        return userRegistryApi.searchUsingPOST(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, new UserSearchDto(fiscalCode));
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
                .onFailure().invoke(x -> log.error("Error during update user status for userId: {}, institutionId: {}, productId:{} -> exception: {}", userId, institutionId, productId, x.getMessage(), x))
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
}
