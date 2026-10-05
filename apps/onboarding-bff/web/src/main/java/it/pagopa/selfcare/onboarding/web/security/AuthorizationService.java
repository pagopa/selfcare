package it.pagopa.selfcare.onboarding.web.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.core.IamService;
import it.pagopa.selfcare.onboarding.core.TokenService;
import it.pagopa.selfcare.onboarding.web.constants.PermissionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    private static final String PAGOPA_ISSUER = "PAGOPA";

    /**
     * Permissions that, for users authenticated with a non-PAGOPA issuer (e.g. SPID/CIE citizens
     * coming from the public FE), can also be granted - as a fallback when IAM denies access - if the
     * user is one of the onboarding's own users, matched by id (e.g. the citizen who submitted the
     * onboarding request, who legitimately has no IAM institution role yet). Management permissions
     * (approve/reject) are intentionally excluded from this fallback and always require a positive IAM
     * check, regardless of issuer.
     */
    private static final Set<String> VIEW_PERMISSIONS_BYPASSABLE_WITHOUT_IAM = Set.of(
            PermissionConstants.SELC_VIEW_ACCOUNT_PAGE,
            PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS
    );

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

        if (!hasPermission
                && !PAGOPA_ISSUER.equalsIgnoreCase(selfCareUser.getIssuer())
                && VIEW_PERMISSIONS_BYPASSABLE_WITHOUT_IAM.contains(permission)) {
            hasPermission = isOnboardingRequester(selfCareUser, onboardingData);
            log.info("IAM denied but non-PAGOPA issuer: applying onboarding-requester fallback, onboardingId={}, userId={}, permission={}, isOnboardingRequester={}",
                    onboardingId, selfCareUser.getId(), permission, hasPermission);
        }

        return hasPermission;
    }

    private boolean isOnboardingRequester(SelfCareUser selfCareUser, OnboardingData onboardingData) {
        String userId = selfCareUser.getId();
        if (StringUtils.isBlank(userId) || onboardingData.getUsers() == null) {
            return false;
        }
        return onboardingData.getUsers().stream()
                .map(User::getId)
                .filter(StringUtils::isNotBlank)
                .anyMatch(userId::equalsIgnoreCase);
    }
}
