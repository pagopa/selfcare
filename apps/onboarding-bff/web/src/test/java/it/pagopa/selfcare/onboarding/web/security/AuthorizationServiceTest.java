package it.pagopa.selfcare.onboarding.web.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.core.IamService;
import it.pagopa.selfcare.onboarding.core.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void hasPermission_withIamGranted_shouldReturnTrueWithoutFallback() {
        // given: IAM alone is enough, the onboarding-membership fallback is never consulted
        String onboardingId = "onboardingId";
        String permission = "Selc:ManageAccountPage";
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

    @Test
    void hasPermission_withViewPermission_andIamDenied_andUserIsOnboardingRequester_shouldFallbackToTrue() {
        // given: the citizen who submitted the onboarding request, who legitimately has no IAM role yet.
        // The fallback is issuer-agnostic: it applies regardless of whether the user authenticated via
        // PAGOPA or SPID/CIE, as long as the permission is view-only and the user is a listed onboarding user.
        String onboardingId = "onboardingId";
        String permission = "Selc:ViewAccountDocuments";
        String userId = "user-id";
        String productId = "product-id";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);
        User user = new User();
        user.setId(userId);
        onboardingData.setUsers(List.of(user));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertTrue(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withViewPermission_andIamDenied_andUserIsNotOnboardingRequester_shouldDeny() {
        // given
        String onboardingId = "onboardingId";
        String permission = "Selc:ViewAccountDocuments";
        String userId = "user-id";
        String productId = "product-id";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);
        User user = new User();
        user.setId("another-user-id");
        onboardingData.setUsers(List.of(user));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertFalse(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withManagePermission_andIamDenied_shouldNeverFallbackEvenIfOnboardingMember() {
        // given: management permissions (approve/reject) must always be IAM-gated, regardless of
        // whether the user is also one of the onboarding's listed users
        String onboardingId = "onboardingId";
        String permission = "Selc:ManageAccountPage";
        String userId = "user-id";
        String productId = "product-id";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);
        User user = new User();
        user.setId(userId);
        onboardingData.setUsers(List.of(user));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertFalse(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }
}
