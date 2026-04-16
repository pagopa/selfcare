package it.pagopa.selfcare.onboarding.service;


import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    private final OnboardingRepository onboardingRepository;

    public UserService(
            OnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    public List<String> findByInstitutionAndProduct(String institutionId, String productId) {
        var onboardings = onboardingRepository.findByOnboardingUsers(institutionId, productId);
        if (onboardings.isEmpty()) {
            return List.of();
        }
        return onboardings.stream()
                .flatMap(onboarding -> onboarding.getUsers().stream())
                .map(User::getId)
                .collect(Collectors.toSet())
                .stream()
                .toList();
    }

    public void deleteByIdAndInstitutionIdAndProductId(String institutionId, String productId) {
        log.debug("Deleting user for institution {} and product {}", institutionId, productId);
        DeletedUserCountResponse response =  institutionApi.deleteUserInstitutionProductUsers(institutionId, productId);
        if (Objects.isNull(response) || response.getDeletedUserCount() < 1L) {
            log.error("Error during user deletion: {}", response);
        }
    }
}
