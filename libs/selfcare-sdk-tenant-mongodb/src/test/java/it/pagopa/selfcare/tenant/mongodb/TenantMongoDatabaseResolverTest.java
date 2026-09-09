package it.pagopa.selfcare.tenant.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import org.junit.jupiter.api.Test;

class TenantMongoDatabaseResolverTest {

    @Test
    void resolve_returnsRegistryDatabaseForCurrentTenant() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantRegistry tenantRegistry = mock(TenantRegistry.class);
        when(tenantContext.requiredTenantId()).thenReturn("PNPG");
        when(tenantRegistry.resolve("PNPG")).thenReturn(new TenantDefinition(
                new TenantDefinition.MongoDefinition(
                        "cosmos-pnpg", "selcOnboardingPnpg", "MONGODB_CONNECTION_STRING_PNPG")));

        TenantMongoDatabaseResolver resolver =
                new TenantMongoDatabaseResolver(tenantRegistry, tenantContext);

        assertEquals("selcOnboardingPnpg", resolver.resolve());
    }
}
