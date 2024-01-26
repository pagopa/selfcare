package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.OnboardedProductMapper;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.*;

import static it.pagopa.selfcare.user.constant.CustomError.*;

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
    private static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,email,name,workContacts";


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
                .transformToUni(obj -> userRegistryApi.findByIdUsingGET("workContacts", obj.getUserId()))
                .merge()
                .filter(userResource -> Objects.nonNull(userResource.getWorkContacts())
                        && userResource.getWorkContacts().containsKey(institutionId))
                .map(user -> user.getWorkContacts().get(institutionId))
                .filter(workContract -> StringUtils.isNotBlank(workContract.getEmail().getValue()))
                .map(workContract -> workContract.getEmail().getValue())
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

}
