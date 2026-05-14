package it.pagopa.selfcare.onboarding.web.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.core.IamService;
import it.pagopa.selfcare.onboarding.core.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private TokenService tokenService;
    @Mock
    private IamService iamService;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void hasPermission_shouldDelegateToTokenService() {
        // given
        String onboardingId = "onboardingId";
        String permission = "Selc:ApproveAccountPage";
        String userId = "user-id";
        String productId = "product-id";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(true);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertTrue(result);
        verify(tokenService, times(1)).getOnboardingWithUserInfo(onboardingId);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }
}
