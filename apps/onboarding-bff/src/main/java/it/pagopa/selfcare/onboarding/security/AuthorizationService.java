package it.pagopa.selfcare.onboarding.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.service.IamService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.PermissionConstants;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    /**
     * Permissions that can also be granted, when IAM denies access, to the users listed in the
     * onboarding itself or the requester identified by {@code userRequester.userRequestUid}. The
     * fallback is independent of the token issuer. Management permissions (approve/reject) are
     * excluded on purpose: they always require a positive IAM check.
     */
    private static final Set<String> VIEW_PERMISSIONS_BYPASSABLE_WITHOUT_IAM = Set.of(
            PermissionConstants.SELC_VIEW_ACCOUNT_PAGE,
            PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS
    );

    private final TokenService tokenService;
    private final IamService iamService;

    public boolean hasPermission(SecurityIdentity identity, String onboardingId, String permission) {
        requireNonNull(identity, "Authentication is required");
        requireNonNull(onboardingId, "OnboardingId is required");
        requireNonNull(permission, "Permission is required");

        String userId = SecurityIdentityUtils.getUid(identity);
        OnboardingData onboardingData = tokenService.getOnboardingWithUserInfo(onboardingId);
        String productId = onboardingData.getProductId();

        log.info("Checking IAM permission: onboardingId={}, userId={}, permission={}, productId={}",
                LogUtils.sanitize(onboardingId), LogUtils.sanitize(userId),
                LogUtils.sanitize(permission), LogUtils.sanitize(productId));
        boolean hasPermission = iamService.hasIamUserPermission(permission, userId, StringUtils.EMPTY, productId);
        log.info("IAM permission check result: onboardingId={}, userId={}, permission={}, productId={}, authorized={}",
                LogUtils.sanitize(onboardingId), LogUtils.sanitize(userId),
                LogUtils.sanitize(permission), LogUtils.sanitize(productId), hasPermission);

        if (!hasPermission && VIEW_PERMISSIONS_BYPASSABLE_WITHOUT_IAM.contains(permission)) {
            hasPermission = isOnboardingRequester(userId, onboardingData);
            log.info("IAM denied: applying onboarding-requester fallback, onboardingId={}, userId={}, permission={}, isOnboardingRequester={}",
                    LogUtils.sanitize(onboardingId), LogUtils.sanitize(userId),
                    LogUtils.sanitize(permission), hasPermission);
        }

        return hasPermission;
    }

    private boolean isOnboardingRequester(String userId, OnboardingData onboardingData) {
        if (StringUtils.isBlank(userId)) {
            return false;
        }
        if (onboardingData.getUserRequester() != null
                && userId.equalsIgnoreCase(onboardingData.getUserRequester().getUserRequestUid())) {
            return true;
        }
        return onboardingData.getUsers().stream()
                .map(User::getId)
                .filter(StringUtils::isNotBlank)
                .anyMatch(userId::equalsIgnoreCase);
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
