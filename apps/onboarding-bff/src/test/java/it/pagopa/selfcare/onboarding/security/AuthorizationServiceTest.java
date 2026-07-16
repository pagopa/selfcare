package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.service.IamService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @InjectMocks
    private AuthorizationService authorizationService;

    @Mock
    private TokenService tokenService;

    @Mock
    private IamService iamService;

    @Mock
    private SecurityIdentity identity;

    @Test
    void hasPermission_validIdentityAndGrantedPermission_returnsTrue() {
        // given
        String onboardingId = "onboarding-id";
        String permission = "MANAGE";
        String userId = "user-uid";
        String productId = "prod-test";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);

        when(identity.<String>getAttribute("uid")).thenReturn(userId);
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, StringUtils.EMPTY, productId)).thenReturn(true);

        // when
        boolean result = authorizationService.hasPermission(identity, onboardingId, permission);

        // then
        assertTrue(result);
    }

    @Test
    void hasPermission_validIdentityAndDeniedPermission_returnsFalse() {
        // given
        String onboardingId = "onboarding-id";
        String permission = "MANAGE";
        String userId = "user-uid";
        String productId = "prod-test";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);

        when(identity.<String>getAttribute("uid")).thenReturn(userId);
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, StringUtils.EMPTY, productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(identity, onboardingId, permission);

        // then
        assertFalse(result);
    }

    @Test
    void hasPermission_nullIdentity_throwsNullPointerException() {
        // given
        String onboardingId = "onboarding-id";
        String permission = "MANAGE";

        // when / then
        assertThrows(NullPointerException.class,
                () -> authorizationService.hasPermission(null, onboardingId, permission));
    }

    @Test
    void hasPermission_nullOnboardingId_throwsNullPointerException() {
        // given
        String permission = "MANAGE";

        // when / then
        assertThrows(NullPointerException.class,
                () -> authorizationService.hasPermission(identity, null, permission));
    }

    @Test
    void hasPermission_nullPermission_throwsNullPointerException() {
        // given
        String onboardingId = "onboarding-id";

        // when / then
        assertThrows(NullPointerException.class,
                () -> authorizationService.hasPermission(identity, onboardingId, null));
    }

    @Test
    void hasPermission_callsIamServiceWithCorrectParameters() {
        // given
        String onboardingId = "onboarding-id";
        String permission = "MANAGE";
        String userId = "user-uid";
        String productId = "prod-test";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);

        when(identity.<String>getAttribute("uid")).thenReturn(userId);
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(eq(permission), eq(userId), eq(StringUtils.EMPTY), eq(productId))).thenReturn(true);

        // when
        authorizationService.hasPermission(identity, onboardingId, permission);

        // then
        verify(iamService).hasIamUserPermission(permission, userId, StringUtils.EMPTY, productId);
    }
}
