package it.pagopa.selfcare.document.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantRegistryConfigTest {

    @Inject
    TenantRegistry tenantRegistry;

    @Test
    void onlyArIsSupported() {
        assertEquals(Set.of("AR"), tenantRegistry.supportedTenantIds());
    }

    @Test
    void arUsesDocumentDatabaseAndItsOwnConnectionString() {
        assertEquals("selcDocument", tenantRegistry.resolve("AR").mongo().database());
        assertEquals("MONGODB_CONNECTION_STRING_AR", tenantRegistry.resolve("ar").mongo().connectionStringEnvVar());
        assertTrue(tenantRegistry.connectionString("AR").isPresent());
    }

    @Test
    void arJwtKeyIsTenantBound() {
        assertEquals("JWT_PUBLIC_KEY_AR", tenantRegistry.resolve("AR").jwt().publicKeyEnvVar());
    }

    @Test
    void unsupportedTenantIsRejected() {
        assertThrows(UnknownTenantException.class, () -> tenantRegistry.resolve("PNPG"));
        assertThrows(UnknownTenantException.class, () -> tenantRegistry.resolve("UNKNOWN"));
    }
}
