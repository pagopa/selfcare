package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.OnboardedProductMapper;
import it.pagopa.selfcare.user.util.QueryUtils;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@ApplicationScoped
public class UserServiceImpl implements UserService {

    @RestClient
    @Inject
    private UserApi userRegistryApi;
    private final OnboardedProductMapper onboardedProductMapper;
    @Inject
    private QueryUtils queryUtils;
    @Inject
    private UserUtils userUtils;
    @Inject
    private UserInstitutionService userInstitutionService;

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,email,name,workContacts";

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
    public Uni<List<UserInstitution>> retrieveBindings(String institutionId, String userId, String[] states) {
        String[] finalStates = states != null && states.length > 0 ? states : null;
        List<OnboardedProductState> relationshipStates = Optional.ofNullable(finalStates)
                .map(userUtils::convertStatesToRelationshipsState)
                .orElse(null);

        UserInstitutionFilter userInstitutionFilter = UserInstitutionFilter.builder()
                .userId(userId)
                .institutionId(institutionId)
                .build();

        OnboardedProductFilter onboardedProductFilter = OnboardedProductFilter.builder()
                .relationshipId(relationshipStates)
                .build();


        Map<String, Object> queryParameter = userInstitutionFilter.constructMap();
        queryParameter.putAll(onboardedProductFilter.constructMap());

        return userInstitutionService.retrieveFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(new ResourceNotFoundException(""))
                .onItem().transformToMulti(Multi.createFrom()::iterable)
                .map(userInstitution -> userUtils.filterProduct(userInstitution, finalStates))
                .collect()
                .asList();
    }
}
