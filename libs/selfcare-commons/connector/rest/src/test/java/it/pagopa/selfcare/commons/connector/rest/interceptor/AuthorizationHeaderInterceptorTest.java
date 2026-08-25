package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationHeaderInterceptorTest {

    private final AuthorizationHeaderInterceptor interceptor;


    AuthorizationHeaderInterceptorTest() {
        interceptor = new AuthorizationHeaderInterceptor();
    }


    @BeforeEach
    void resetContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    void apply_nullAuthentication() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        assertNull(headers.get(HttpHeaders.AUTHORIZATION));
    }


    @Test
    void apply_notNullAuthentication() {
        // given
        String credentials = "credentials";
        TestingAuthenticationToken auth = new TestingAuthenticationToken("principal", credentials);
        SecurityContextHolder.getContext().setAuthentication(auth);
        RequestTemplate requestTemplate = new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Collection<String> headerValues = headers.get(HttpHeaders.AUTHORIZATION);
        assertNotNull(headerValues);
        assertEquals(1, headerValues.size());
        Optional<String> headerValue = headerValues.stream().findAny();
        assertTrue(headerValue.isPresent());
        assertEquals("Bearer " + credentials, headerValue.get());
    }


    @Test
    void apply_nullRequestAttributes() {
        // given
        RequestTemplate requestTemplate = new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        assertNull(headers.get(HttpHeaders.AUTHORIZATION));
    }


    @Test
    void apply_notNullRequestAttributes() {
        // given
        String authorizationValue = "auth value";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationValue);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate requestTemplate = new RequestTemplate();

        // when
        interceptor.apply(requestTemplate);

        // then
        Map<String, Collection<String>> headers = requestTemplate.headers();
        Collection<String> headerValues = headers.get(HttpHeaders.AUTHORIZATION);
        assertNotNull(headerValues);
        assertEquals(1, headerValues.size());
        Optional<String> headerValue = headerValues.stream().findAny();
        assertTrue(headerValue.isPresent());
        assertEquals(authorizationValue, headerValue.get());
    }
}