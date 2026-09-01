package it.pagopa.selfcare.onboarding.connector.rest.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import feign.RequestTemplate;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TenantHeaderInterceptorTest {

    private TenantHeaderInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantHeaderInterceptor();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void propagatesTenantHeaderFromCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "AR");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        Collection<String> tenantHeaders = template.headers().get("X-Tenant-Id");
        assertEquals(1, tenantHeaders.size());
        assertEquals("AR", tenantHeaders.iterator().next());
    }

    @Test
    void doesNotAddTenantHeaderWhenRequestContextIsMissing() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertNull(template.headers().get("X-Tenant-Id"));
    }
}
