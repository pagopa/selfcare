package it.pagopa.selfcare.onboarding.web.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.core.IamService;
import it.pagopa.selfcare.onboarding.core.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    private final TokenService tokenService;
    private final IamService iamService;

    public boolean hasPermission(Authentication authentication, String onboardingId, String permission) {
        Assert.notNull(authentication, "Authentication is required");
        Assert.notNull(onboardingId, "OnboardingId is required");
        Assert.notNull(permission, "Permission is required");

        SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();
        OnboardingData onboardingData = tokenService.getOnboardingWithUserInfo(onboardingId);
        String productId = onboardingData.getProductId();
        log.info("Checking IAM permission: onboardingId={}, userId={}, permission={}, productId={}",
                onboardingId, selfCareUser.getId(), permission, productId);
        boolean hasPermission = iamService.hasIamUserPermission(permission, selfCareUser.getId(), StringUtils.EMPTY, productId);
        log.info("IAM permission check result: onboardingId={}, userId={}, permission={}, productId={}, authorized={}",
                onboardingId, selfCareUser.getId(), permission, productId, hasPermission);
        return hasPermission;
    }
}
