package it.pagopa.selfcare.onboarding.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.service.IamService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    private final TokenService tokenService;
    private final IamService iamService;

    public boolean hasPermission(SecurityIdentity identity, String onboardingId, String permission) {
        Objects.requireNonNull(identity, "SecurityIdentity is required");
        Objects.requireNonNull(onboardingId, "OnboardingId is required");
        Objects.requireNonNull(permission, "Permission is required");

        String userId = identity.getAttribute("uid");
        OnboardingData onboardingData = tokenService.getOnboardingWithUserInfo(onboardingId);
        String productId = onboardingData.getProductId();
        log.info("Checking IAM permission: onboardingId={}, userId={}, permission={}, productId={}",
                onboardingId, userId, permission, productId);
        boolean hasPermission = iamService.hasIamUserPermission(permission, userId, StringUtils.EMPTY, productId);
        log.info("IAM permission check result: authorized={}", hasPermission);
        return hasPermission;
    }
}
