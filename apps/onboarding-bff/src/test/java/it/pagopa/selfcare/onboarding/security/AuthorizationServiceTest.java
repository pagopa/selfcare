package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserRequester;
import it.pagopa.selfcare.onboarding.service.IamService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
        // given
        String onboardingId = "onboarding\r\nid";
        String userId = "user\r\nid";
        String permission = "permission\r\nname";
        String productId = "product\r\nid";
        OnboardingData data = new OnboardingData();
        data.setProductId(productId);
        when(tokenService.getOnboardingWithUserInfo(onboardingId)).thenReturn(Uni.createFrom().item(data));
        givenIam(permission, userId, productId, true);

        // when
        boolean authorized = check(identityOf(userId), onboardingId, permission);

        // then
        assertTrue(authorized);
        verify(tokenService).getOnboardingWithUserInfo(onboardingId);
        verify(iamService).hasIamUserPermission(permission, userId, "", productId);
    }

    @Test
    void hasPermission_withIamGranted_shouldReturnTrueWithoutFallback() {
        // given
        givenOnboardingWithUsers();
        givenIam(MANAGE_PAGE, USER_ID, PRODUCT_ID, true);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, MANAGE_PAGE);

        // then
        assertTrue(authorized);
        verify(tokenService, times(1)).getOnboardingWithUserInfo(ONBOARDING_ID);
        verify(iamService, times(1)).hasIamUserPermission(MANAGE_PAGE, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withViewDocuments_andIamDenied_andUserIsOnboardingRequester_shouldFallbackToTrue() {
        // given
        givenOnboardingWithUsers(USER_ID);
        givenIam(VIEW_DOCUMENTS, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertTrue(authorized);
        verify(iamService, times(1)).hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withViewPage_andIamDenied_andUserIsOnboardingRequester_shouldFallbackToTrue() {
        // given
        givenOnboardingWithUsers("another-user-id", USER_ID);
        givenIam(VIEW_PAGE, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE);

        // then
        assertTrue(authorized);
    }

    @Test
    void hasPermission_withViewPermission_andIamDenied_andUserIsNotOnboardingRequester_shouldDeny() {
        // given
        givenOnboardingWithUsers("another-user-id");
        givenIam(VIEW_DOCUMENTS, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertFalse(authorized);
        verify(iamService, times(1)).hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withManagePermission_andIamDenied_shouldNeverFallbackEvenIfOnboardingMember() {
        // given
        givenOnboardingWithUsers(USER_ID);
        givenIam(MANAGE_PAGE, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, MANAGE_PAGE);

        // then
        assertFalse(authorized);
        verify(iamService, times(1)).hasIamUserPermission(MANAGE_PAGE, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void hasPermission_withUnlistedPermission_andIamDenied_shouldNeverFallbackEvenIfOnboardingMember() {
        // given
        givenOnboardingWithUsers(USER_ID);
        givenIam("Selc:ViewSomethingElse", USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, "Selc:ViewSomethingElse");

        // then
        assertFalse(authorized);
    }

    @Test
    void hasPermission_withViewPermission_shouldMatchRequesterIdIgnoringCase() {
        // given
        givenOnboardingWithUsers("USER-ID");
        givenIam(VIEW_DOCUMENTS, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertTrue(authorized);
    }

    @Test
    void hasPermission_withViewPermission_shouldSkipBlankAndNullUserIds() {
        // given
        givenOnboardingWithUsers(null, "", "  ", USER_ID);
        givenIam(VIEW_DOCUMENTS, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertTrue(authorized);
    }

    @Test
    void hasPermission_withViewPermission_andBlankCallerId_shouldDenyEvenIfUsersHaveBlankIds() {
        // given
        givenOnboardingWithUsers("", " ", USER_ID);
        givenIam(VIEW_DOCUMENTS, "", PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(""), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertFalse(authorized);
    }

    @Test
    void hasPermission_withViewPermission_andTokenWithoutUid_shouldDeny() {
        // given
        givenOnboardingWithUsers(USER_ID);
        givenIam(VIEW_DOCUMENTS, null, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(null), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertFalse(authorized);
    }

    @Test
    void hasPermission_withViewPermission_andOnboardingWithoutUsers_shouldDeny() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(PRODUCT_ID);
        onboardingData.setUsers(null);
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(onboardingData));
        givenIam(VIEW_PAGE, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE);

        // then
        assertFalse(authorized);
    }

    @Test
    void hasPermission_whenIamFails_shouldPropagateTheErrorWithoutFallback() {
        // given
        givenOnboardingWithUsers(USER_ID);
        WebApplicationException failure = new WebApplicationException(503);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        Uni<Boolean> permission = authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> permission.await().indefinitely());
        assertSame(failure, thrown);
    }

    @Test
    void hasPermission_whenOnboardingCannotBeRead_shouldNotCallIam() {
        // given
        WebApplicationException failure = new WebApplicationException(404);
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(Uni.createFrom().failure(failure));

        // when
        Uni<Boolean> permission = authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> permission.await().indefinitely());
        assertSame(failure, thrown);
        verify(iamService, never()).hasIamUserPermission(any(), any(), any(), any());
    }

    @Test
    void hasPermission_withMissingArguments_shouldBeRejectedBeforeAnyLookup() {
        // given
        SecurityIdentity identity = identityOf(USER_ID);

        // when / then the precondition failures are thrown at once, not carried by the result
        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.hasPermission(null, ONBOARDING_ID, VIEW_PAGE));
        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.hasPermission(identity, null, VIEW_PAGE));
        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.hasPermission(identity, ONBOARDING_ID, null));
        verifyNoMoreInteractions(tokenService, iamService);
    }

    @Test
    void hasPermission_pendingStages_neverStartTheNextOneEarly() {
        // given the onboarding lookup and the IAM check are both pending
        AtomicReference<UniEmitter<? super OnboardingData>> onboardingEmitter = new AtomicReference<>();
        AtomicReference<UniEmitter<? super Boolean>> iamEmitter = new AtomicReference<>();
        OnboardingData data = new OnboardingData();
        data.setProductId(PRODUCT_ID);
        data.setUsers(List.of());
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().<OnboardingData>emitter(onboardingEmitter::set));
        when(iamService.hasIamUserPermission(VIEW_PAGE, USER_ID, "", PRODUCT_ID))
                .thenReturn(Uni.createFrom().<Boolean>emitter(iamEmitter::set));
        Boolean[] outcome = new Boolean[1];

        // when the permission is requested
        authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE)
                .subscribe().with(result -> outcome[0] = result);

        // then IAM waits for the onboarding
        assertNull(outcome[0]);
        verify(iamService, never()).hasIamUserPermission(any(), any(), any(), any());

        // when the onboarding arrives
        onboardingEmitter.get().complete(data);

        // then IAM is asked, once, and the permission is still pending
        verify(iamService, times(1)).hasIamUserPermission(VIEW_PAGE, USER_ID, "", PRODUCT_ID);
        assertNull(outcome[0]);

        // when IAM answers
        iamEmitter.get().complete(Boolean.TRUE);

        // then
        assertEquals(Boolean.TRUE, outcome[0]);
        verify(tokenService, times(1)).getOnboardingWithUserInfo(ONBOARDING_ID);
    }

    @Test
    void hasPermission_requesterFallbackIsAppliedOnlyAfterIamDenies() {
        // given
        AtomicReference<UniEmitter<? super Boolean>> iamEmitter = new AtomicReference<>();
        givenOnboardingWithUsers(USER_ID);
        when(iamService.hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID))
                .thenReturn(Uni.createFrom().<Boolean>emitter(iamEmitter::set));
        Boolean[] outcome = new Boolean[1];

        // when IAM is still pending
        authorizationService.hasPermission(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS)
                .subscribe().with(result -> outcome[0] = result);

        // then the requester is not granted in the meantime
        assertNull(outcome[0]);

        // when IAM denies
        iamEmitter.get().complete(Boolean.FALSE);

        // then the fallback grants the requester
        assertEquals(Boolean.TRUE, outcome[0]);
    }

    @Test
    void requesterUidGrantsViewPermissionWithoutOnboardingUsers() {
        // given
        givenOnboardingWithRequester(USER_ID);
        givenIam(VIEW_DOCUMENTS, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertTrue(authorized);
        verify(iamService).hasIamUserPermission(VIEW_DOCUMENTS, USER_ID, "", PRODUCT_ID);
    }

    @Test
    void requesterUidMatchesIgnoringCaseForViewPage() {
        // given
        givenOnboardingWithRequester("USER-ID", "another-user");
        givenIam(VIEW_PAGE, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE);

        // then
        assertTrue(authorized);
    }

    @Test
    void requesterUidNeverGrantsManagementPermission() {
        // given
        givenOnboardingWithRequester(USER_ID);
        givenIam(MANAGE_PAGE, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, MANAGE_PAGE);

        // then
        assertFalse(authorized);
    }

    @Test
    void mismatchedRequesterWithoutUsersIsDenied() {
        // given
        givenOnboardingWithRequester("another-user");
        givenIam(VIEW_PAGE, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_PAGE);

        // then
        assertFalse(authorized);
    }

    @Test
    void missingRequesterUidStillChecksOnboardingUsers() {
        // given
        givenOnboardingWithRequester(null, null, "USER-ID");
        givenIam(VIEW_DOCUMENTS, USER_ID, PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(USER_ID), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertTrue(authorized);
    }

    @Test
    void blankCallerCannotMatchBlankRequesterUid() {
        // given
        givenOnboardingWithRequester(" ", " ");
        givenIam(VIEW_DOCUMENTS, " ", PRODUCT_ID, false);

        // when
        boolean authorized = check(identityOf(" "), ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertFalse(authorized);
    }

    @Test
    void identityWithoutJwtPrincipalHasNoUid() {
        // given
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal("uid")).build();
        givenOnboardingWithUsers("uid");
        givenIam(VIEW_DOCUMENTS, null, PRODUCT_ID, false);

        // when
        boolean authorized = check(identity, ONBOARDING_ID, VIEW_DOCUMENTS);

        // then
        assertFalse(authorized);
    }

    private boolean check(SecurityIdentity identity, String onboardingId, String permission) {
        return authorizationService.hasPermission(identity, onboardingId, permission).await().indefinitely();
    }

    private void givenIam(String permission, String userId, String productId, boolean granted) {
        when(iamService.hasIamUserPermission(permission, userId, "", productId))
                .thenReturn(Uni.createFrom().item(granted));
    }

    private void givenOnboardingWithRequester(String requesterUid, String... userIds) {
        OnboardingData data = givenOnboardingWithUsers(userIds);
        UserRequester requester = new UserRequester();
        requester.setUserRequestUid(requesterUid);
        data.setUserRequester(requester);
    }

    private OnboardingData givenOnboardingWithUsers(String... userIds) {
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId(PRODUCT_ID);
        List<User> users = new ArrayList<>();
        for (String id : userIds) {
            User user = new User();
            user.setId(id);
            users.add(user);
        }
        onboardingData.setUsers(users);
        when(tokenService.getOnboardingWithUserInfo(ONBOARDING_ID)).thenReturn(Uni.createFrom().item(onboardingData));
        return onboardingData;
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
}
