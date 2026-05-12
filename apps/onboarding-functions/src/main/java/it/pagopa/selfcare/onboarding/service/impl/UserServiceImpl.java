package it.pagopa.selfcare.onboarding.service.impl;


import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    public void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId) {
        log.debug("Deleting user for institution {} and product {}", institutionId, productId);
        DeletedUserCountResponse response =  institutionApi.deleteUserInstitutionProductUsers(institutionId, productId);
        if (Objects.isNull(response) || response.getDeletedUserCount() < 1L) {
            log.error("Error during user deletion: {}", response);
        }
    }
}
