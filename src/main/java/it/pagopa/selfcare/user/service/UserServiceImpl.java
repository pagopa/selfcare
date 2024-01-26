package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.OnboardedProductMapper;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static it.pagopa.selfcare.user.constant.CustomError.USER_NOT_FOUND_ERROR;
import static it.pagopa.selfcare.user.constant.CustomError.USER_TO_UPDATE_NOT_FOUND;
import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;

@RequiredArgsConstructor
@ApplicationScoped
public class UserServiceImpl implements UserService {

    @RestClient
    @Inject
    private UserApi userRegistryApi;
    private final OnboardedProductMapper onboardedProductMapper;
    private final UserInstitutionMapper userInstitutionMapper;
    private final UserInstitutionService userInstitutionService;
    private static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,email,name,workContacts";
    private static final String WORK_CONTACTS = "workContacts";
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public Uni<List<String>> getUsersEmails(String institutionId, String productId) {
        var userInstitutionFilters = constructUserInstitutionFilterMap(institutionId);
        var productFilters = constructOnboardedProductFilterMap(productId);
        Multi<UserInstitution> userInstitutions =  userInstitutionService.findAllWithFilter(retrieveMapForFilter(userInstitutionFilters, productFilters));
        return userInstitutions.onItem()
                .transformToUni(obj -> userRegistryApi.findByIdUsingGET(WORK_CONTACTS, obj.getUserId()))
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
        Multi<UserInstitution> userInstitutions =  UserInstitution.find("institutionId", institutionId).stream();
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
        Map<String, Object> queryParameter = buildQueryParams(userId, productId, institutionId);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(() -> {
                    log.error(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId));
                    return new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode());
                })
                .onItem().transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId()))
                .onFailure(this::checkIfNotFoundException).transform(t -> new ResourceNotFoundException(String.format(USER_NOT_FOUND_ERROR.getMessage(), userId), USER_NOT_FOUND_ERROR.getCode()));
    }

    @Override
    public Multi<UserInstitutionResponse> findAllUserInstitutions(String institutionId,
                                                                  String userId,
                                                                  List<String> roles,
                                                                  List<String> states,
                                                                  List<String> products,
                                                                  List<String> productRoles) {
        var userInstitutionFilters = constructUserInstitutionFilterMap(institutionId, userId);
        var productFilters = constructOnboardedProductFilterMap(products, states, roles, productRoles);
        Multi<UserInstitution> userInstitutions =  userInstitutionService.findAllWithFilter(retrieveMapForFilter(userInstitutionFilters, productFilters));
        return userInstitutions.onItem().transform(userInstitutionMapper::toResponse);
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

    private Map<String, Object> buildQueryParams(String userId, String productId, String institutionId) {
        OnboardedProductFilter onboardedProductFilter = OnboardedProductFilter.builder()
                .productId(productId)
                .build();

        UserInstitutionFilter userInstitutionFilter = UserInstitutionFilter.builder()
                .userId(userId)
                .institutionId(institutionId)
                .build();

        Map<String, Object> filterMap = userInstitutionFilter.constructMap();
        filterMap.putAll(onboardedProductFilter.constructMap());
        return filterMap;
    }

    private Map<String, Object> constructUserInstitutionFilterMap(String institutionId, String userId) {
        return UserInstitutionFilter
                .builder()
                .institutionId(institutionId)
                .userId(userId)
                .build()
                .constructMap();
    }

    private Map<String, Object> constructOnboardedProductFilterMap(List<String> products,
                                                                   List<String> states,
                                                                   List<String> roles,
                                                                   List<String> productRoles) {
        return OnboardedProductFilter.builder()
                .productId(formatQueryParameterList(products))
                .role(formatQueryParameterList(roles))
                .status(formatQueryParameterList(states))
                .productRole(formatQueryParameterList(productRoles))
                .build()
                .constructMap();
    }

    private Map<String, Object> constructUserInstitutionFilterMap(String institutionId) {
        return UserInstitutionFilter
                .builder()
                .institutionId(institutionId)
                .build()
                .constructMap();
    }

    private Map<String, Object> constructOnboardedProductFilterMap(String productId) {
        return OnboardedProductFilter.builder()
                .productId(productId)
                .build()
                .constructMap();
    }

    private Map<String, Object> retrieveMapForFilter(Map<String, Object> ... maps) {
        Map<String, Object> map = new HashMap<>();
        Arrays.stream(maps).forEach(map::putAll);
        return map;
    }

    private boolean checkIfNotFoundException(Throwable throwable) {
        if(throwable instanceof WebClientApplicationException wex) {
            return wex.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
        }

        return false;
    }
}
