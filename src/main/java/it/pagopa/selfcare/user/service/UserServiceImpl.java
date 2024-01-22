package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@ApplicationScoped
public class UserServiceImpl implements UserService {

    public Multi<UserProductResponse> getUserProductsByInstitution(String institutionId) {
        Multi<UserInstitution> userInstitutions =  UserInstitution.find("institutionId", institutionId).stream();
        return userInstitutions.onItem()
                .transformToUni(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId())
                        .map(userResource -> UserProductResponse.builder()
                                .id(userResource.getId().toString())
                                .name(userResource.getName().getValue())
                                .email(userResource.getEmail().getValue())
                                .surname(userResource.getFamilyName().getValue())
                                .taxCode(userResource.getFiscalCode())
                                .products(onboardedProductMapper.toList(userInstitution.getProducts()))
                                .build()))
                .merge();
    }
}
