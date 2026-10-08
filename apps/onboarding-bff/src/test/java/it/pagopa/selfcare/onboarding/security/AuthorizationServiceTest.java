package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.service.IamService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same cases of the Spring {@code AuthorizationServiceTest}, plus the edge cases of the requester
 * fallback. The caller is identified by the {@code uid} claim of a JWT principal, not by an identity attribute.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final String ONBOARDING_ID = "onboardingId";
    private static final String PRODUCT_ID = "product-id";
    private static final String USER_ID = "user-id";
    private static final String VIEW_PAGE = "Selc:ViewAccountPage";
    private static final String VIEW_DOCUMENTS = "Selc:ViewAccountDocuments";
    private static final String MANAGE_PAGE = "Selc:ManageAccountPage";

    @Mock
    private TokenService tokenService;

    @Mock
    private IamService iamService;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void sanitizingLogsDoesNotChangeTheValuesSentToIam() {
        String onboardingId = "onboarding\r\nid";
        String userId = "user\r\nid";
        String permission = "permission\r\nname";
        String productId = "product\r\nid";
        OnboardingData data = new OnboardingData();
        data.setProductId(productId);
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(data);
        when(iamService.hasIamUserPermission(permission, userId, "", productId)).thenReturn(true);

        assertTrue(authorizationService.hasPermission(identityOf(userId), onboardingId, permission));

        verify(tokenService).getOnboardingWithUserInfo(onboardingId);
        verify(iamService).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withIamGranted_shouldReturnTrueWithoutFallback() {
        givenOnboardingWithUsers();
        when(iamService.hasIamUserPermission(MANAGE_PAGE, USER_ID, "", PRODUCT_ID)).thenReturn(true);

        assertTrue(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, MANAGE_PAGE));

        verify(tokenService, times(1)).getOnboardingWithUserInfo(ONBOARDING_ID);
        verify(iamService, times(1)).hasIamUserPermission(MANAGE_PAGE, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withViewDocuments_andIamDenied_andUserIsOnboardingRequester_shouldFallbackToTrue() {
        givenOnboardingWithUsers(USER_ID);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertTrue(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS));

        verify(iamService, times(1)).hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withViewPage_andIamDenied_andUserIsOnboardingRequester_shouldFallbackToTrue() {
        givenOnboardingWithUsers("another-user-id", USER_ID);
        when(iamService.hasIamUserPermission(VIEW_PAGE, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertTrue(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE));
    }

    @Test
    void hasPermission_withViewPermission_andIamDenied_andUserIsNotOnboardingRequester_shouldDeny() {
        givenOnboardingWithUsers("another-user-id");
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS));

        verify(iamService, times(1)).hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withManagePermission_andIamDenied_shouldNeverFallbackEvenIfOnboardingMember() {
        givenOnboardingWithUsers(USER_ID);
        when(iamService.hasIamUserPermission(MANAGE_PAGE, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, MANAGE_PAGE));

        verify(iamService, times(1)).hasIamUserPermission(MANAGE_PAGE, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withUnlistedPermission_andIamDenied_shouldNeverFallbackEvenIfOnboardingMember() {
        givenOnboardingWithUsers(USER_ID);
        when(iamService.hasIamUserPermission("Selc:ViewSomethingElse", USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, "Selc:ViewSomethingElse"));
    }

    @Test
    void hasPermission_withViewPermission_shouldMatchRequesterIdIgnoringCase() {
        givenOnboardingWithUsers("USER-ID");
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertTrue(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS));
    }

    @Test
    void hasPermission_withViewPermission_shouldSkipBlankAndNullUserIds() {
        givenOnboardingWithUsers(null, "", "  ", USER_ID);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertTrue(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS));
    }

    @Test
    void hasPermission_withViewPermission_andBlankCallerId_shouldDenyEvenIfUsersHaveBlankIds() {
        givenOnboardingWithUsers("", " ", USER_ID);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, "", "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identityOf(""), ONBOARDING_ID, VIEW_DOCUMENTS));
    }

    @Test
    void hasPermission_withViewPermission_andTokenWithoutUid_shouldDeny() {
        givenOnboardingWithUsers(USER_ID);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, null, "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identityOf(null), ONBOARDING_ID, VIEW_DOCUMENTS));
    }

    @Test
    void hasPermission_withViewPermission_andOnboardingWithoutUsers_shouldDeny() {
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(PRODUCT_ID);
        onboardingData.setUsers(null);
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(onboardingData);
        when(iamService.hasIamUserPermission(VIEW_PAGE, USER_ID, "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE));
    }

    @Test
    void hasPermission_whenIamFails_shouldPropagateTheErrorWithoutFallback() {
        givenOnboardingWithUsers(USER_ID);
        WebApplicationException failure = new WebApplicationException(503);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID)).thenThrow(failure);

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS));

        assertEquals(failure, thrown);
    }

    @Test
    void hasPermission_whenOnboardingCannotBeRead_shouldNotCallIam() {
        WebApplicationException failure = new WebApplicationException(404);
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)).thenThrow(failure);

        assertThrows(WebApplicationException.class,
                () -> authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS));

        verify(iamService, never()).hasIamUserPermission(any(), any(), any(), any());
    }

    @Test
    void hasPermission_withMissingArguments_shouldBeRejected() {
        SecurityIdentity identity = identityOf(USER_ID);

        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.hasPermission(null, ONBOARDING_ID, VIEW_PAGE));
        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.hasPermission(identity, null, VIEW_PAGE));
        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.hasPermission(identity, ONBOARDING_ID, null));
        verifyNoMoreInteractions(tokenService, iamService);
    }

    private void givenOnboardingWithUsers(String... userIds) {
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(PRODUCT_ID);
        List<User> users = new ArrayList<>();
        for (String id : userIds) {
            User user = new User();
            user.setId(id);
            users.add(user);
        }
        onboardingData.setUsers(users);
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(onboardingData);
    }

    /** Identity backed by a JWT principal: the user id is the {@code uid} claim, the principal name is not set. */
    private static SecurityIdentity identityOf(String uid) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        if (uid != null) {
            claims.setClaim("uid", uid);
        }
        return QuarkusSecurityIdentity.builder().setPrincipal(new DefaultJWTCallerPrincipal(claims)).build();
    }

    @Test
    void identityWithoutJwtPrincipalHasNoUid() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal("uid")).build();
        givenOnboardingWithUsers("uid");
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, null, "", PRODUCT_ID)).thenReturn(false);

        assertFalse(authorizationService.hasPermission(identity, ONBOARDING_ID, VIEW_DOCUMENTS));
    }
}
