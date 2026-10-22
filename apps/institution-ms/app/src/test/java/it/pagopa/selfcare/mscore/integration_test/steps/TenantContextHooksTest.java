package it.pagopa.selfcare.mscore.integration_test.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.mongodb.TenantMongoDatabaseFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TenantContextHooksTest {

    @Test
    void setsDefaultTenantForFixtureAccessAndClearsAfterScenario() {
        TenantContext tenantContext = new TenantContext();
        TenantContextHooks hooks = new TenantContextHooks(tenantContext, "AR");

        hooks.setTenantContext();

        assertEquals("AR", tenantContext.requiredTenantId());
        hooks.clearTenantContext();
        assertFalse(tenantContext.tenantId().isPresent());
    }

    @Test
    void clearsContextAfterScenarioFailure() {
        TenantContext tenantContext = new TenantContext();
        TenantContextHooks hooks = new TenantContextHooks(tenantContext, "AR");
        hooks.setTenantContext();

        try {
            throw new IllegalStateException("fixture failed");
        } catch (IllegalStateException exception) {
            hooks.clearTenantContext();
        }

        assertFalse(tenantContext.tenantId().isPresent());
    }

    @Test
    void fixtureContextSelectsConfiguredTenantDatabase() {
        TenantContext tenantContext = new TenantContext();
        TenantContextHooks hooks = new TenantContextHooks(tenantContext, "PNPG");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://localhost:27017")
                .withProperty("MONGO_PNPG", "mongodb://localhost:27017");
        TenantRegistry registry = new TenantRegistry(
                new ObjectMapper(),
                environment,
                """
                {
                  "AR":{"mongo":{"account":"ar","database":"institution-ar","connectionStringEnvVar":"MONGO_AR"}},
                  "PNPG":{"mongo":{"account":"pnpg","database":"institution-pnpg","connectionStringEnvVar":"MONGO_PNPG"}}
                }
                """,
                "AR,PNPG",
                "");
        registry.initialize();
        TenantMongoDatabaseFactory databaseFactory =
                new TenantMongoDatabaseFactory(registry, tenantContext);

        try {
            hooks.setTenantContext();
            assertEquals("institution-pnpg", databaseFactory.getMongoDatabase().getName());
        } finally {
            hooks.clearTenantContext();
        }
        assertFalse(tenantContext.tenantId().isPresent());
    }
}
