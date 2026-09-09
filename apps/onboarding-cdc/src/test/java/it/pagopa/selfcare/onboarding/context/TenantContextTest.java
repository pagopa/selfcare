package it.pagopa.selfcare.onboarding.context;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class TenantContextTest {

    @Test
    void resolveReturnsDefaultTenantWhenBlank() {
        assertEquals("PNPG", TenantContext.resolve(null));
        assertEquals("PNPG", TenantContext.resolve(""));
        assertEquals("PNPG", TenantContext.resolve("  "));
    }

    @Test
    void resolveReturnsSupportedTenant() {
        assertEquals("AR", TenantContext.resolve("AR"));
    }

    @Test
    void resolveThrowsOnUnsupportedTenant() {
        assertThrows(IllegalArgumentException.class, () -> TenantContext.resolve("UNKNOWN"));
    }

    @Test
    void currentTenantOrDefaultReturnsDefaultWhenNoScope() {
        assertNull(TenantContext.currentTenant());
        assertEquals("PNPG", TenantContext.currentTenantOrDefault());
    }

    @Test
    void openSetsAndRestoresTenant() {
        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertEquals("AR", TenantContext.currentTenant());
        }
        assertNull(TenantContext.currentTenant());
    }
}
