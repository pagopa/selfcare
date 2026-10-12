package it.pagopa.selfcare.onboarding.context;

import com.microsoft.azure.functions.HttpRequestMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TenantContextTest {

    @Test
    void tenantHeaderMatchesAzureLowercaseHeaderName() {
        // given
        HttpRequestMessage<?> request = requestWithHeaders(Map.of("x-tenant-id", "AR"));

        // when
        String tenant = TenantContext.tenantHeader(request);

        // then
        assertEquals("AR", tenant);
    }

    @Test
    void tenantHeaderMatchesCanonicalHeaderName() {
        // given
        HttpRequestMessage<?> request = requestWithHeaders(Map.of("X-Tenant-Id", "PNPG"));

        // when
        String tenant = TenantContext.tenantHeader(request);

        // then
        assertEquals("PNPG", tenant);
    }

    @Test
    void tenantHeaderReturnsNullWhenHeadersAreMissing() {
        // given
        HttpRequestMessage<?> request = requestWithHeaders(null);

        // when
        String tenant = TenantContext.tenantHeader(request);

        // then
        assertNull(tenant);
    }

    private static HttpRequestMessage<?> requestWithHeaders(Map<String, String> headers) {
        HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
        doReturn(headers).when(request).getHeaders();
        return request;
    }
}
