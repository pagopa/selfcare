package it.pagopa.selfcare.onboarding.service.impl;


import it.pagopa.selfcare.onboarding.dto.UserMail;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import org.openapi.quarkus.user_json.model.User;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    public List<UserMail> findEmailByInstitutionAndProducts(String institutionId, List<String> products) {
        List<UserInstitutionResponse> userInstitutionResponses = institutionApi.retrieveUserInstitutions(
                institutionId,
                null,
                products,
                null,
                null,
                null);

        if (Objects.isNull(userInstitutionResponses) || userInstitutionResponses.isEmpty()) {
            log.error("No users found for institution '{}' and products '{}'", institutionId, products);
            return List.of();
        }

        List<UserMail> emails = userInstitutionResponses.stream()
                .filter(userInstitutionResponse -> StringUtils.isNotBlank(userInstitutionResponse.getUserMailUuid()))
                .map(userInstitutionResponse -> UserMail.builder()
                        .userId(userInstitutionResponse.getUserId())
                        .userMailUuid(userInstitutionResponse.getUserMailUuid())
                        .build())
                .toList();

        if (emails.isEmpty()) {
            log.error("No email found for institution '{}' and products '{}'", institutionId, products);
            return List.of();
        }

        return emails;
    }
}
