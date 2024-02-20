package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
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
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.user.constant.CustomError.*;
import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;
import static it.pagopa.selfcare.user.util.UserUtils.VALID_USER_PRODUCT_STATES_FOR_NOTIFICATION;

@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class UserServiceImpl implements UserService {

    @RestClient
    @Inject
    private UserApi userRegistryApi;

    private final OnboardedProductMapper onboardedProductMapper;
    private final UserUtils userUtils;
    private final UserInstitutionService userInstitutionService;
    private final UserInstitutionMapper userInstitutionMapper;
    private final UserMapper userMapper;

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
                .asList().onItem().transform(userInstitutions -> userInstitutions.stream().map(userInstitutionMapper::toResponse).collect(Collectors.toList()));
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
