package it.pagopa.selfcare.onboarding.service.impl;


import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.dto.UserMail;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.api.UserApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Slf4j
public class UserServiceImpl implements UserService {

    private final InstitutionApi institutionApi;
    private final UserApi userApi;

    @Inject
    public UserServiceImpl(@RestClient InstitutionApi institutionApi, @RestClient UserApi userApi) {
        this.institutionApi = institutionApi;
        this.userApi = userApi;
    }

    public void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId) {
        log.debug("Deleting user for institution {} and product {}", institutionId, productId);
        DeletedUserCountResponse response = institutionApi.deleteUserInstitutionProductUsers(institutionId, productId);
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

    @Override
    public List<UserInstitutionResponse> getActiveManagersByInstitutionAndProduct(
            String institutionId,
            String productId,
            OnboardedProductResponse.StatusEnum status) {
        log.debug("Retrieving active managers: institutionId={}, productId={}, status={}", institutionId, productId, status);
        return institutionApi.retrieveUserInstitutions(
                institutionId,
                null,
                List.of(productId),
                List.of(String.valueOf(PartyRole.MANAGER)),
                List.of(String.valueOf(status)),
                null);
    }

    @Override
    public void sendMailRequest(String userId, SendMailDto sendMailDto) {
        log.debug("Sending mail request to user service: userId={}", userId);
        try {
            userApi.sendMailRequest(userId, sendMailDto);
        } catch (Exception e) {
            log.error("Impossible to send mail to user {}", userId, e);
        }
    }
}
