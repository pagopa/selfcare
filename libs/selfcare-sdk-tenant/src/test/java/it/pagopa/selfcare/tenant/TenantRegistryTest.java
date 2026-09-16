package it.pagopa.selfcare.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TenantRegistryTest {

    @Test
    void sanitizeConnectionString_decodesHtmlAmpersands() {
        String encoded =
                "mongodb://user:pwd@selc-d-cosmosdb-mongodb-account.mongo.cosmos.azure.com:10255/"
                        + "selcOnboarding?ssl=true&amp;replicaSet=globaldb&amp;retrywrites=false"
                        + "&amp;maxIdleTimeMS=120000&amp;appName=@selc-d-cosmosdb-mongodb-account@";

        String sanitized = TenantRegistry.sanitizeConnectionString(encoded);

        assertEquals(
                "mongodb://user:pwd@selc-d-cosmosdb-mongodb-account.mongo.cosmos.azure.com:10255/"
                        + "selcOnboarding?ssl=true&replicaSet=globaldb&retrywrites=false"
                        + "&maxIdleTimeMS=120000&appName=@selc-d-cosmosdb-mongodb-account@",
                sanitized);
    }

    /**
     * Regression test for a JSON corruption bug: when {@code tenant.registry.json} is wrapped in a
     * {@code ${VAR:<literal JSON default>}} SmallRye Config expression, the expression parser
     * miscounts nesting depth for 2+ levels of literal braces in the default and silently drops a
     * closing brace, merging the second tenant ("PNPG") into the first tenant's "mongo" object.
     * The companion fix keeps tenant.registry.json un-wrapped in application.properties; this test
     * guards the expected two-tenant JSON shape (mongo + jwt per tenant) itself.
     */
    @Test
    void initialize_parsesAllTenantsWithMongoAndJwtConfig() {
        String json =
                "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcOnboarding\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"},"
                        + "\"jwt\":{\"publicKeyEnvVar\":\"JWT_PUBLIC_KEY_AR\"}},"
                        + "\"PNPG\":{\"mongo\":{\"account\":\"cosmos-pnpg\",\"database\":\"selcOnboarding\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_PNPG\"},"
                        + "\"jwt\":{\"publicKeyEnvVar\":\"JWT_PUBLIC_KEY_PNPG\"}}}";

        TenantRegistry registry = new TenantRegistry();
        registry.tenantRegistryJson = json;
        registry.supportedTenants = "AR,PNPG";
        System.setProperty("MONGODB_CONNECTION_STRING_AR", "mongodb://ar");
        System.setProperty("MONGODB_CONNECTION_STRING_PNPG", "mongodb://pnpg");
        try {
            registry.initialize();
        } finally {
            System.clearProperty("MONGODB_CONNECTION_STRING_AR");
            System.clearProperty("MONGODB_CONNECTION_STRING_PNPG");
        }

        assertEquals(2, registry.definitions().size());
        TenantDefinition ar = registry.definitions().get("AR");
        TenantDefinition pnpg = registry.definitions().get("PNPG");
        assertNotNull(pnpg, "PNPG must be parsed as its own top-level tenant, not merged into AR");
        assertEquals("cosmos-ar", ar.mongo().account());
        assertEquals("cosmos-pnpg", pnpg.mongo().account());
        assertNotNull(ar.jwt(), "AR must keep its own jwt config, not have it dropped/merged");
        assertEquals("JWT_PUBLIC_KEY_AR", ar.jwt().publicKeyEnvVar());
        assertEquals("JWT_PUBLIC_KEY_PNPG", pnpg.jwt().publicKeyEnvVar());
    }
}
