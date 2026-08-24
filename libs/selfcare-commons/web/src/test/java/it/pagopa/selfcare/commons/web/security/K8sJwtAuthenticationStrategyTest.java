package it.pagopa.selfcare.commons.web.security;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthenticationV1Api;
import io.kubernetes.client.openapi.models.V1TokenReviewBuilder;
import io.kubernetes.client.openapi.models.V1TokenReviewStatusBuilder;
import io.kubernetes.client.openapi.models.V1UserInfoBuilder;
import it.pagopa.selfcare.commons.base.security.ServiceAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class K8sJwtAuthenticationStrategyTest {

    private static final String MDC_UID = "uid";

    private final K8sJwtAuthenticationStrategy k8sJwtAuthenticationStrategy;
    private final AuthenticationV1Api apiClientMock;


    public K8sJwtAuthenticationStrategyTest() {
        apiClientMock = mock(AuthenticationV1Api.class);
        k8sJwtAuthenticationStrategy = new K8sJwtAuthenticationStrategy(apiClientMock);
    }


    @BeforeEach
    public void reset() {
        Mockito.reset(apiClientMock);
    }


    @AfterEach
    void cleanUp() {
        MDC.clear();
    }


    @Test
    void authenticate_nullAuth() {
        // given
        final JwtAuthenticationToken authentication = null;
        // when
        final Executable executable = () -> k8sJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertThrows(JwtAuthenticationException.class, executable);
        verifyNoInteractions(apiClientMock);
    }


    @Test
    void authenticate_apiClientException() throws ApiException {
        // given
        final String token = "token";
        final JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        doThrow(RuntimeException.class)
                .when(apiClientMock)
                .createTokenReview(any(), any(), any(), any());
        // when
        final Executable executable = () -> k8sJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertThrows(JwtAuthenticationException.class, executable);
        verify(apiClientMock, times(1))
                .createTokenReview(argThat(argument -> token.equals(argument.getSpec().getToken())), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(apiClientMock);
    }


    @Test
    void authenticate_invalidToken() throws ApiException {
        // given
        final String token = "token";
        final JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        when(apiClientMock.createTokenReview(any(), any(), any(), any()))
                .thenReturn(new V1TokenReviewBuilder()
                        .withStatus(new V1TokenReviewStatusBuilder()
                                .withAuthenticated(false)
                                .build())
                        .build());
        // when
        final Executable executable = () -> k8sJwtAuthenticationStrategy.authenticate(authentication);
        // then
        final JwtAuthenticationException exception = assertThrows(JwtAuthenticationException.class, executable);
        assertNotNull(exception.getCause());
        assertEquals("Invalid token status", exception.getCause().getMessage());
        verify(apiClientMock, times(1))
                .createTokenReview(argThat(argument -> token.equals(argument.getSpec().getToken())), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(apiClientMock);
    }


    @Test
    void authenticate_nullUid() throws ApiException {
        // given
        final String token = "token";
        final JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        when(apiClientMock.createTokenReview(any(), any(), any(), any()))
                .thenReturn(new V1TokenReviewBuilder()
                        .withStatus(new V1TokenReviewStatusBuilder()
                                .withAuthenticated(true)
                                .withUser(new V1UserInfoBuilder()
                                        .build())
                                .build())
                        .build());
        // when
        final Executable executable = () -> k8sJwtAuthenticationStrategy.authenticate(authentication);
        // then
        final JwtAuthenticationException exception = assertThrows(JwtAuthenticationException.class, executable);
        assertNotNull(exception.getCause());
        assertEquals("Service Account id is required", exception.getCause().getMessage());
        verify(apiClientMock, times(1))
                .createTokenReview(argThat(argument -> token.equals(argument.getSpec().getToken())), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(apiClientMock);
    }


    @Test
    void authenticate() throws ApiException {
        // given
        final String token = "token";
        final String uid = UUID.randomUUID().toString();
        final String username = "service-account";
        final JwtAuthenticationToken notAuthenticatedToken = new JwtAuthenticationToken(token);
        when(apiClientMock.createTokenReview(any(), any(), any(), any()))
                .thenReturn(new V1TokenReviewBuilder()
                        .withStatus(new V1TokenReviewStatusBuilder()
                                .withAuthenticated(true)
                                .withUser(new V1UserInfoBuilder()
                                        .withUid(uid)
                                        .withUsername(username)
                                        .build())
                                .build())
                        .build());
        // when
        final JwtAuthenticationToken authenticatedToken = k8sJwtAuthenticationStrategy.authenticate(notAuthenticatedToken);
        // then
        assertEquals(uid, MDC.get(MDC_UID));
        assertNotNull(authenticatedToken);
        assertEquals(token, authenticatedToken.getCredentials());
        assertEquals(ServiceAccount.class, authenticatedToken.getPrincipal().getClass());
        assertEquals(uid, ((ServiceAccount) authenticatedToken.getPrincipal()).getId());
        assertEquals(username, ((ServiceAccount) authenticatedToken.getPrincipal()).getUsername());
        assertNotNull(authenticatedToken.getAuthorities());
        verify(apiClientMock, times(1))
                .createTokenReview(argThat(argument -> token.equals(argument.getSpec().getToken())), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(apiClientMock);
    }

}