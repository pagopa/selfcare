package it.pagopa.selfcare.tenant.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import org.junit.jupiter.api.Test;

class TenantMongoDatabaseResolverTest {

    @Test
    void resolve_returnsRegistryDatabaseForCurrentTenant() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        when(tenantContext.requiredTenantId()).thenReturn("PNPG");
        when(tenantRegistry.resolve("PNPG")).thenReturn(new TenantDefinition(
                new TenantDefinition.MongoDefinition(
                        "cosmos-pnpg", "selcOnboardingPnpg", "MONGODB_CONNECTION_STRING_PNPG"),
                null));

        TenantMongoDatabaseResolver resolver =
                new TenantMongoDatabaseResolver(tenantRegistry, tenantContext);

        assertEquals("selcOnboardingPnpg", resolver.resolve());
    }

    @Test
    void resolve_reselectsDatabaseForInterleavedTenants() {
        TenantContext tenantContext = new TenantContext();
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        when(tenantRegistry.resolve("AR")).thenReturn(new TenantDefinition(
                new TenantDefinition.MongoDefinition(
                        "cosmos-ar", "selcAuthAr", "MONGODB_CONNECTION_STRING_AR"),
                null));
        when(tenantRegistry.resolve("PNPG")).thenReturn(new TenantDefinition(
                new TenantDefinition.MongoDefinition(
                        "cosmos-pnpg", "selcAuthPnpg", "MONGODB_CONNECTION_STRING_PNPG"),
                null));
        TenantMongoDatabaseResolver resolver =
                new TenantMongoDatabaseResolver(tenantRegistry, tenantContext);

        tenantContext.setTenantId("AR");
        assertEquals("selcAuthAr", resolver.resolve());
        tenantContext.setTenantId("PNPG");
        assertEquals("selcAuthPnpg", resolver.resolve());
        tenantContext.clear();
        assertThrows(UnresolvedTenantException.class, resolver::resolve);
    }
}
