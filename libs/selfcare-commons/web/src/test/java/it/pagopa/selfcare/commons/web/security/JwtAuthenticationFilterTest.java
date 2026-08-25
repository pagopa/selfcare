package it.pagopa.selfcare.commons.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.web.model.Problem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private static final HttpServletResponse RESPONSE_MOCK = mock(HttpServletResponse.class);
    private static final FilterChain FILTER_CHAIN_MOCK = mock(FilterChain.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationManager authenticationManagerMock;
    private final ObjectMapper objectMapperSpy;

    private ArgumentCaptor<JwtAuthenticationToken> jwtAuthenticationTokenCaptor;


    public JwtAuthenticationFilterTest() {
        objectMapperSpy = spy(new ObjectMapper());
        jwtAuthenticationTokenCaptor = ArgumentCaptor.forClass(JwtAuthenticationToken.class);
        authenticationManagerMock = mock(AuthenticationManager.class);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManagerMock, objectMapperSpy);
    }

    @Test
    void doFilterInternal_authHeaderEmpty() throws ServletException, IOException {
        // given
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        // when
        jwtAuthenticationFilter.doFilterInternal(requestMock, RESPONSE_MOCK, FILTER_CHAIN_MOCK);
        // then
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManagerMock, times(1))
                .authenticate(jwtAuthenticationTokenCaptor.capture());
        Assertions.assertNotNull(jwtAuthenticationTokenCaptor.getValue());
        Assertions.assertNull(jwtAuthenticationTokenCaptor.getValue().getCredentials());
        verifyNoMoreInteractions(authenticationManagerMock);
    }


    @Test
    void doFilterInternal_authHeaderNotEmptyButNotBearer() throws ServletException, IOException {
        // given
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn("token");
        // when
        jwtAuthenticationFilter.doFilterInternal(requestMock, RESPONSE_MOCK, FILTER_CHAIN_MOCK);
        // then
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManagerMock, times(1))
                .authenticate(jwtAuthenticationTokenCaptor.capture());
        Assertions.assertNotNull(jwtAuthenticationTokenCaptor.getValue());
        Assertions.assertNull(jwtAuthenticationTokenCaptor.getValue().getCredentials());
        verifyNoMoreInteractions(authenticationManagerMock);
    }


    @Test
    void doFilterInternal_invalidToken() throws ServletException, IOException {
        // given
        String token = "token";
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn("Bearer " + token);
        // when
        jwtAuthenticationFilter.doFilterInternal(requestMock, RESPONSE_MOCK, FILTER_CHAIN_MOCK);
        // then
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManagerMock, times(1))
                .authenticate(jwtAuthenticationTokenCaptor.capture());
        Assertions.assertNotNull(jwtAuthenticationTokenCaptor.getValue());
        Assertions.assertEquals(token, jwtAuthenticationTokenCaptor.getValue().getCredentials());
        verifyNoMoreInteractions(authenticationManagerMock);
    }


    @Test
    void doFilterInternal_withBearerTokenButAuthKo() throws ServletException, IOException {
        // given
        String token = "token";
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn("Bearer " + token);
        doThrow(JwtAuthenticationException.class)
                .when(authenticationManagerMock)
                .authenticate(any());
        String mdcKey = "key";
        MDC.put(mdcKey, "val");
        // when
        jwtAuthenticationFilter.doFilterInternal(requestMock, RESPONSE_MOCK, FILTER_CHAIN_MOCK);
        // then
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManagerMock, times(1))
                .authenticate(jwtAuthenticationTokenCaptor.capture());
        Assertions.assertNotNull(jwtAuthenticationTokenCaptor.getValue());
        Assertions.assertEquals(token, jwtAuthenticationTokenCaptor.getValue().getCredentials());
        Assertions.assertNull(MDC.get(mdcKey));
        verifyNoMoreInteractions(authenticationManagerMock);
    }

    @Test
    void doFilterInternal_witAuthOkButAuthoritiesFailed() throws ServletException, IOException {
        // given
        String token = "token";
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn("Bearer " + token);
        doThrow(AuthoritiesRetrieverException.class)
                .when(authenticationManagerMock)
                .authenticate(any());
        when(RESPONSE_MOCK.getOutputStream())
                .thenReturn(new DelegatingServletOutputStream(ServletOutputStream.nullOutputStream()));
        String mdcKey = "key";
        MDC.put(mdcKey, "val");
        // when
        jwtAuthenticationFilter.doFilterInternal(requestMock, RESPONSE_MOCK, FILTER_CHAIN_MOCK);
        // then
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManagerMock, times(1))
                .authenticate(jwtAuthenticationTokenCaptor.capture());
        Assertions.assertNotNull(jwtAuthenticationTokenCaptor.getValue());
        Assertions.assertEquals(token, jwtAuthenticationTokenCaptor.getValue().getCredentials());
        Assertions.assertNull(MDC.get(mdcKey));
        final ArgumentCaptor<Problem> problemArgumentCaptor = ArgumentCaptor.forClass(Problem.class);
        verify(objectMapperSpy, times(1))
                .writeValueAsString(problemArgumentCaptor.capture());
        Assertions.assertNotNull(problemArgumentCaptor.getValue());
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemArgumentCaptor.getValue().getStatus());
        verifyNoMoreInteractions(authenticationManagerMock);
    }


    @Test
    void doFilterInternal_validJwtAndAuthOk() throws ServletException, IOException {
        // given
        String token = "token";
        String mdcKey = "key";
        String mdcVal = "val";
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn("Bearer " + token);
        when(authenticationManagerMock.authenticate(any()))
                .thenAnswer(invocationOnMock -> {
                    MDC.put(mdcKey, mdcVal);
                    return new TestingAuthenticationToken("username", "password");
                });
        // when
        jwtAuthenticationFilter.doFilterInternal(requestMock, RESPONSE_MOCK, FILTER_CHAIN_MOCK);
        // then
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManagerMock, times(1))
                .authenticate(jwtAuthenticationTokenCaptor.capture());
        Assertions.assertNotNull(jwtAuthenticationTokenCaptor.getValue());
        Assertions.assertEquals(token, jwtAuthenticationTokenCaptor.getValue().getCredentials());
        Assertions.assertNull(MDC.get(mdcKey));
        verifyNoMoreInteractions(authenticationManagerMock);
    }

}
