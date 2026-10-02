package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestTemplate;
import it.pagopa.selfcare.commons.tenant.TenantContext;
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
        request.addHeader("X-Tenant-Id", "AR");
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
        assertEquals("AR", headers.get("X-Tenant-Id").iterator().next());
    }

    @Test
    void apply_usesValidatedTenantContextInsteadOfIncomingHeader() {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        AuthorizationHeaderInterceptor tenantAwareInterceptor =
                new AuthorizationHeaderInterceptor(tenantContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "PNPG");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.header("X-Tenant-Id", "STALE");

        tenantAwareInterceptor.apply(requestTemplate);

        assertEquals(
                java.util.List.of("AR"),
                requestTemplate.headers().get("X-Tenant-Id").stream().toList());
    }

    @Test
    void applyRejectsTenantAwareOutboundCallWithoutResolvedTenant() {
        AuthorizationHeaderInterceptor tenantAwareInterceptor =
                new AuthorizationHeaderInterceptor(new TenantContext());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "AR");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThrows(
                it.pagopa.selfcare.commons.tenant.UnresolvedTenantException.class,
                () -> tenantAwareInterceptor.apply(new RequestTemplate()));
    }
}