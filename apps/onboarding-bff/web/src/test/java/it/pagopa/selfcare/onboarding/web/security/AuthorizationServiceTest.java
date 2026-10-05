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
    void hasPermission_shouldDelegateToTokenService() {
        // given
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
    void hasPermission_withPagopaIssuer_andIamDenied_shouldNeverFallback() {
        // given: Google-federated (PAGOPA issuer) users must always go through IAM, no fallback ever
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
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).issuer("PAGOPA").build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertFalse(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withSpidIssuerAndIamGranted_shouldReturnTrueWithoutFallback() {
        // given: e.g. an institution admin (already has an IAM role) viewing documents of a
        // pending onboarding they didn't submit themselves - IAM alone is enough, no fallback needed.
        String onboardingId = "onboardingId";
        String permission = "Selc:ViewAccountDocuments";
        String userId = "user-id";
        String productId = "product-id";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(productId);
        onboardingData.setUsers(List.of());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).issuer("SPID").build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(true);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertTrue(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withSpidIssuerAndViewPermission_andIamDenied_andUserIsOnboardingRequester_shouldFallbackToTrue() {
        // given: the citizen who submitted the onboarding request, who legitimately has no IAM role yet
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
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).issuer("SPID").build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertTrue(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withSpidIssuerAndViewPermission_andIamDenied_andUserIsNotOnboardingRequester_shouldDeny() {
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
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).issuer("SPID").build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertFalse(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withSpidIssuerAndManagePermission_andIamDenied_shouldNeverFallback() {
        // given: management permissions must always be IAM-gated, regardless of onboarding membership
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
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).issuer("SPID").build());
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(false);

        // when
        boolean result = authorizationService.hasPermission(authentication, onboardingId, permission);

        // then
        assertFalse(result);
        verify(iamService, times(1)).hasIamUserPermission(permission, userId, "", productId);
    }
}
