package it.pagopa.selfcare.onboarding.context;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@QuarkusTest
class TenantContextTest {

    @Test
    void tenantHeaderMatchesLowercaseName() {
        assertEquals("AR", TenantContext.tenantHeader(requestWithHeaders(Map.of("x-tenant-id", "AR"))));
    }

    @Test
    void tenantHeaderMatchesMixedCaseName() {
        assertEquals("AR", TenantContext.tenantHeader(requestWithHeaders(Map.of("X-TeNaNt-iD", "AR"))));
    }

    @Test
    void tenantHeaderMatchesCanonicalName() {
        assertEquals("AR", TenantContext.tenantHeader(requestWithHeaders(Map.of("X-Tenant-Id", "AR"))));
    }

    @Test
    void tenantHeaderIgnoresOtherHeaders() {
        assertNull(TenantContext.tenantHeader(requestWithHeaders(Map.of("x-tenant", "AR"))));
    }

    @Test
    void tenantHeaderNullHeaders() {
        assertNull(TenantContext.tenantHeader(requestWithHeaders(null)));
    }

    @Test
    void openAcceptsLowercaseHeader() {
        try (TenantContext.Scope ignored =
                TenantContext.open(requestWithHeaders(Map.of("x-tenant-id", "ar")), executionContext())) {
            assertEquals("AR", TenantContext.currentTenant());
        }
        assertNull(TenantContext.currentTenant());
    }

    @Test
    void openRejectsMissingHeader() {
        HttpRequestMessage<?> request = requestWithHeaders(Map.of());
        ExecutionContext context = executionContext();
        assertThrows(IllegalArgumentException.class, () -> TenantContext.open(request, context));
    }

    @Test
    void openRejectsNullHeaders() {
        HttpRequestMessage<?> request = requestWithHeaders(null);
        ExecutionContext context = executionContext();
        assertThrows(IllegalArgumentException.class, () -> TenantContext.open(request, context));
    }

    @Test
    void resolveAcceptsOnlyRegistryTenants() {
        assertEquals("AR", TenantContext.resolve(" ar "));
        assertEquals("PNPG", TenantContext.resolve("pnpg"));
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> TenantContext.resolve("UNKNOWN"));
        assertEquals("Unsupported tenant", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> TenantContext.resolve(" "));
    }

    @Test
    void alternatingTenantsOnSameThreadRestorePreviousTenant() {
        try (TenantContext.Scope ar = TenantContext.open("AR")) {
            try (TenantContext.Scope pnpg = TenantContext.open("PNPG")) {
                assertEquals("PNPG", TenantContext.requiredTenant());
            }
            assertEquals("AR", TenantContext.requiredTenant());
        }
        try (TenantContext.Scope pnpg = TenantContext.open("PNPG")) {
            assertEquals("PNPG", TenantContext.requiredTenant());
        }
        assertNull(TenantContext.currentTenant());
    }

    private static HttpRequestMessage<?> requestWithHeaders(Map<String, String> headers) {
        HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
        doReturn(headers).when(request).getHeaders();
        return request;
    }

    private static ExecutionContext executionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        return context;
    }
}
