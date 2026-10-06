package it.pagopa.selfcare.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantRegistryTest {

    private static final String AR_MONGO =
            "\"mongo\":{\"account\":\"ar\",\"database\":\"db\",\"connectionStringEnvVar\":\"SDK_TENANT_TEST_MONGO_AR\"}";
    private static final String PNPG_MONGO =
            "\"mongo\":{\"account\":\"pnpg\",\"database\":\"db\",\"connectionStringEnvVar\":\"SDK_TENANT_TEST_MONGO_PNPG\"}";

    @Test
    void mongoIsRequiredByDefaultEvenWhenCredentialDimensionsExist() {
        TenantRegistry registry = newRegistry("{\"AR\":{\"userRegistry\":"
                + "{\"apiKeyEnvVar\":\"SDK_TENANT_TEST_UR_AR\"}}}", "AR", "");
        assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                .getMessage().contains("Mongo"));
    }

    @Test
    void mongoOptionalAllowsCredentialOnlyResourceRegistryForSupportedTenants() {
        TenantRegistry registry = newRegistry("{\"AUTH\":{\"authEnabled\":true}}", "AR,PNPG", "");
        registry.tenantResourcesRegistryJson = Optional.of("{\"AR\":{"
                + "\"oneIdentity\":{\"clientIdEnvVar\":\"SDK_TENANT_TEST_OI_ID\","
                + "\"clientSecretEnvVar\":\"SDK_TENANT_TEST_OI_SECRET\"},"
                + "\"userRegistry\":{\"apiKeyEnvVar\":\"SDK_TENANT_TEST_UR_AR\"}},"
                + "\"PNPG\":{\"userRegistry\":{\"apiKeyEnvVar\":\"SDK_TENANT_TEST_UR_PNPG\"}}}");
        registry.mongoMandatory = false;
        registry.mandatoryOneIdentityTenants = Optional.of("AR");
        registry.mandatoryUserRegistryTenants = Optional.of("AR,PNPG");
        System.setProperty("SDK_TENANT_TEST_OI_ID", "id");
        System.setProperty("SDK_TENANT_TEST_OI_SECRET", "secret");
        System.setProperty("SDK_TENANT_TEST_UR_AR", "ar-key");
        System.setProperty("SDK_TENANT_TEST_UR_PNPG", "pnpg-key");
        try {
            registry.initialize();
            assertEquals("id", registry.oneIdentityCredentials("AR").orElseThrow().clientId());
            assertEquals("pnpg-key", registry.userRegistryCredentials("PNPG").orElseThrow().apiKey());
            assertEquals("pnpg-key", registry.userRegistryApiKey("PNPG"));
            assertTrue(registry.oneIdentityCredentials("PNPG").isEmpty());
            assertThrows(IllegalStateException.class, () -> registry.connectionString("AR"));
            assertThrows(UnknownTenantException.class, () -> registry.resolve("AUTH"));
            assertThrows(UnknownTenantException.class, () -> registry.userRegistryApiKey("AUTH"));
            System.clearProperty("SDK_TENANT_TEST_UR_PNPG");
            assertThrows(IllegalStateException.class, () -> registry.userRegistryApiKey("PNPG"));
        } finally {
            System.clearProperty("SDK_TENANT_TEST_OI_ID");
            System.clearProperty("SDK_TENANT_TEST_OI_SECRET");
            System.clearProperty("SDK_TENANT_TEST_UR_AR");
            System.clearProperty("SDK_TENANT_TEST_UR_PNPG");
        }
    }

    @Test
    void mongoOptionalStillValidatesMongoIfConfigured() {
        TenantRegistry registry = newRegistry("{\"AR\":{" + AR_MONGO + "}}", "AR", "");
        registry.mongoMandatory = false;
        assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                .getMessage().contains("Mongo connection string"));
    }

    @Test
    void resourceRegistryOverridesAuthMetadataAndValidatesOnlySupportedTenants() {
        TenantRegistry registry = newRegistry("{\"AUTH\":{\"authEnabled\":true}}", "AR", "");
        registry.tenantResourcesRegistryJson = Optional.of("{\"AR\":{" + AR_MONGO
                + "},\"PNPG\":{\"oneIdentity\":{\"clientIdEnvVar\":\"INVALID REF\"}}}");
        System.setProperty("SDK_TENANT_TEST_MONGO_AR", "mongodb://ar");
        try {
            registry.initialize();
            assertEquals("ar", registry.resolve("AR").mongo().account());
            assertThrows(UnknownTenantException.class, () -> registry.resolve("PNPG"));
            assertEquals(2, registry.definitions().size());
            registry.tenantResourcesRegistryJson = Optional.of("{\"AR\":{}}");
            assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                    .getMessage().contains("Mongo"));
            registry.tenantResourcesRegistryJson = Optional.of("{invalid}");
            assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                    .getMessage().contains("Invalid tenant registry"));
        } finally {
            System.clearProperty("SDK_TENANT_TEST_MONGO_AR");
        }
    }

    @Test
    void blankResourceOverrideFallsBackToExistingRegistry() {
        TenantRegistry registry = newRegistry("{\"AR\":{" + AR_MONGO + "}}", "AR", "");
        registry.tenantResourcesRegistryJson = Optional.of("  ");
        System.setProperty("SDK_TENANT_TEST_MONGO_AR", "mongodb://ar");
        try {
            registry.initialize();
            assertEquals("ar", registry.resolve("AR").mongo().account());
        } finally {
            System.clearProperty("SDK_TENANT_TEST_MONGO_AR");
        }
    }

    @Test
    void absentOptionalPropertiesDoNotRequireResourcesOrMandatoryDimensions() {
        TenantRegistry registry = newRegistry("{\"AR\":{" + AR_MONGO + "}}", "AR", "");
        registry.tenantResourcesRegistryJson = Optional.empty();
        registry.mandatoryStorageKeys = Optional.empty();
        registry.mandatoryOneIdentityTenants = Optional.empty();
        registry.mandatoryUserRegistryTenants = Optional.empty();
        System.setProperty("SDK_TENANT_TEST_MONGO_AR", "mongodb://ar");
        try {
            registry.initialize();
            assertEquals("ar", registry.resolve("AR").mongo().account());
            assertTrue(registry.mandatoryStorageKeys().isEmpty());
            assertTrue(registry.oneIdentityCredentials("AR").isEmpty());
            assertTrue(registry.userRegistryCredentials("AR").isEmpty());
        } finally {
            System.clearProperty("SDK_TENANT_TEST_MONGO_AR");
        }
    }

    @Test
    void credentials_resolveBothTenantsWithoutRequiringPnpgOneIdentity() {
        String json = "{\"AR\":{" + AR_MONGO
                + ",\"oneIdentity\":{\"clientIdEnvVar\":\"SDK_TENANT_TEST_OI_ID\","
                + "\"clientSecretEnvVar\":\"SDK_TENANT_TEST_OI_SECRET\"},"
                + "\"userRegistry\":{\"apiKeyEnvVar\":\"SDK_TENANT_TEST_UR_AR\"}},"
                + "\"PNPG\":{" + PNPG_MONGO
                + ",\"userRegistry\":{\"apiKeyEnvVar\":\"SDK_TENANT_TEST_UR_PNPG\"}}}";
        TenantRegistry registry = newRegistry(json, "AR,PNPG", "");
        registry.mandatoryOneIdentityTenants = Optional.of("ar");
        registry.mandatoryUserRegistryTenants = Optional.of("AR,PNPG");
        System.setProperty("SDK_TENANT_TEST_MONGO_AR", "mongodb://ar");
        System.setProperty("SDK_TENANT_TEST_MONGO_PNPG", "mongodb://pnpg");
        System.setProperty("SDK_TENANT_TEST_OI_ID", "id");
        System.setProperty("SDK_TENANT_TEST_OI_SECRET", "secret");
        System.setProperty("SDK_TENANT_TEST_UR_AR", "ar-key");
        System.setProperty("SDK_TENANT_TEST_UR_PNPG", "pnpg-key");
        try {
            registry.initialize();
            assertEquals("id", registry.oneIdentityCredentials("ar").orElseThrow().clientId());
            assertEquals("secret", registry.oneIdentityCredentials("AR").orElseThrow().clientSecret());
            assertEquals("ar-key", registry.userRegistryCredentials("AR").orElseThrow().apiKey());
            assertEquals("pnpg-key", registry.userRegistryCredentials("pnpg").orElseThrow().apiKey());
            assertTrue(registry.oneIdentityCredentials("PNPG").isEmpty());
            assertFalse(registry.oneIdentityCredentials("AR").orElseThrow().toString().contains("secret"));
            assertFalse(registry.userRegistryCredentials("AR").orElseThrow().toString().contains("ar-key"));
            assertThrows(UnknownTenantException.class, () -> registry.oneIdentityCredentials("UNKNOWN"));
            assertThrows(UnknownTenantException.class, () -> registry.userRegistryCredentials("UNKNOWN"));
            System.clearProperty("SDK_TENANT_TEST_OI_SECRET");
            assertThrows(IllegalStateException.class, () -> registry.oneIdentityCredentials("AR"));
        } finally {
            System.clearProperty("SDK_TENANT_TEST_MONGO_AR");
            System.clearProperty("SDK_TENANT_TEST_MONGO_PNPG");
            System.clearProperty("SDK_TENANT_TEST_OI_ID");
            System.clearProperty("SDK_TENANT_TEST_OI_SECRET");
            System.clearProperty("SDK_TENANT_TEST_UR_AR");
            System.clearProperty("SDK_TENANT_TEST_UR_PNPG");
        }
    }

    @Test
    void absentOptionalDimensionsReturnEmptyButMandatoryOnesFailStartup() {
        TenantRegistry registry = newRegistry("{\"AR\":{" + AR_MONGO + "}}", "AR", "");
        System.setProperty("SDK_TENANT_TEST_MONGO_AR", "mongodb://ar");
        try {
            registry.initialize();
            assertTrue(registry.oneIdentityCredentials("AR").isEmpty());
            assertTrue(registry.userRegistryCredentials("AR").isEmpty());
            assertTrue(assertThrows(IllegalStateException.class, () -> registry.userRegistryApiKey("AR"))
                    .getMessage().contains("UserRegistry"));
            registry.mandatoryOneIdentityTenants = Optional.of("AR");
            assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                    .getMessage().contains("OneIdentity"));
            registry.mandatoryOneIdentityTenants = Optional.empty();
            registry.mandatoryUserRegistryTenants = Optional.of("AR");
            assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                    .getMessage().contains("UserRegistry"));
            registry.mandatoryUserRegistryTenants = Optional.of("PNPG");
            assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                    .getMessage().contains("not supported"));
        } finally {
            System.clearProperty("SDK_TENANT_TEST_MONGO_AR");
        }
    }

    @Test
    void configuredDimensionsRequireCompleteValidReferencesAndNonblankValues() {
        System.setProperty("SDK_TENANT_TEST_MONGO_AR", "mongodb://ar");
        try {
            String[] invalidDimensions = {
                    "\"oneIdentity\":{}",
                    "\"oneIdentity\":{\"clientIdEnvVar\":\"SDK_TENANT_TEST_MISSING_ID\","
                            + "\"clientSecretEnvVar\":\"SDK_TENANT_TEST_MISSING_SECRET\"}",
                    "\"oneIdentity\":{\"clientIdEnvVar\":\"SDK_TENANT_TEST_ID\"}",
                    "\"oneIdentity\":{\"clientIdEnvVar\":\"BAD ID\","
                            + "\"clientSecretEnvVar\":\"SDK_TENANT_TEST_MISSING_SECRET\"}",
                    "\"oneIdentity\":{\"clientIdEnvVar\":\"SDK_TENANT_TEST_ID\","
                            + "\"clientSecretEnvVar\":\"${BAD_REFERENCE}\"}",
                    "\"userRegistry\":{}",
                    "\"userRegistry\":{\"apiKeyEnvVar\":\"SDK_TENANT_TEST_MISSING_KEY\"}",
                    "\"userRegistry\":{\"apiKeyEnvVar\":\"BAD KEY\"}"
            };
            for (String dimension : invalidDimensions) {
                TenantRegistry registry = newRegistry("{\"AR\":{" + AR_MONGO + "," + dimension + "}}", "AR", "");
                assertThrows(IllegalStateException.class, registry::initialize, dimension);
            }
            System.setProperty("SDK_TENANT_TEST_ID", "id");
            System.setProperty("SDK_TENANT_TEST_MISSING_SECRET", "  ");
            TenantRegistry blank = newRegistry("{\"AR\":{" + AR_MONGO
                    + ",\"oneIdentity\":{\"clientIdEnvVar\":\"SDK_TENANT_TEST_ID\","
                    + "\"clientSecretEnvVar\":\"SDK_TENANT_TEST_MISSING_SECRET\"}}}", "AR", "");
            assertThrows(IllegalStateException.class, blank::initialize);
        } finally {
            System.clearProperty("SDK_TENANT_TEST_MONGO_AR");
            System.clearProperty("SDK_TENANT_TEST_ID");
            System.clearProperty("SDK_TENANT_TEST_MISSING_SECRET");
        }
    }

    @Test
    void duplicateNormalizedTenantsFailStartup() {
        TenantRegistry registry = newRegistry("{\"AR\":{" + AR_MONGO + "},\" ar \":{" + AR_MONGO + "}}", "AR", "");
        assertTrue(assertThrows(IllegalStateException.class, registry::initialize)
                .getMessage().contains("Invalid tenant registry"));
    }

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
        assertTrue(ar.storages().isEmpty());
    }

    @Test
    void initialize_parsesMultipleStorageBindingsPerTenant() {
        String json =
                "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcOnboarding\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"},"
                        + "\"storages\":{\"products\":{\"account\":\"stselcarproducts\",\"container\":\"selc-d-product\","
                        + "\"pathPrefix\":\"\",\"authentication\":{\"type\":\"CONNECTION_STRING\","
                        + "\"connectionStringEnvVar\":\"BLOB_STORAGE_CONN_STRING_AR_PRODUCTS\"}},"
                        + "\"contracts\":{\"account\":\"stselcardocuments\",\"container\":\"contracts\","
                        + "\"pathPrefix\":\"onboarding\",\"authentication\":{\"type\":\"MANAGED_IDENTITY\","
                        + "\"managedIdentityClientIdEnvVar\":\"AZURE_CLIENT_ID_AR_CONTRACTS\"}}}},"
                        + "\"PNPG\":{\"mongo\":{\"account\":\"cosmos-pnpg\",\"database\":\"selcOnboarding\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_PNPG\"},"
                        + "\"storages\":{\"products\":{\"account\":\"stpnpgproducts\",\"container\":\"selc-d-product\","
                        + "\"authentication\":{\"type\":\"CONNECTION_STRING\","
                        + "\"connectionStringEnvVar\":\"BLOB_STORAGE_CONN_STRING_PNPG_PRODUCTS\"}}}}}";

        TenantRegistry registry = newRegistry(json, "AR,PNPG", "products");
        System.setProperty("MONGODB_CONNECTION_STRING_AR", "mongodb://ar");
        System.setProperty("MONGODB_CONNECTION_STRING_PNPG", "mongodb://pnpg");
        System.setProperty("BLOB_STORAGE_CONN_STRING_AR_PRODUCTS", "UseDevelopmentStorage=true");
        System.setProperty("BLOB_STORAGE_CONN_STRING_PNPG_PRODUCTS", "UseDevelopmentStorage=true");
        System.setProperty("AZURE_CLIENT_ID_AR_CONTRACTS", "mi-contracts");
        try {
            registry.initialize();
            TenantDefinition.StorageDefinition arProducts = registry.storage("AR", "products");
            TenantDefinition.StorageDefinition arContracts = registry.storage("ar", "CONTRACTS");
            TenantDefinition.StorageDefinition pnpgProducts = registry.storage("PNPG", "products");
            assertEquals("stselcarproducts", arProducts.account());
            assertEquals("selc-d-product", arProducts.container());
            assertEquals(StorageAuthenticationType.CONNECTION_STRING, arProducts.authentication().type());
            assertEquals("stselcardocuments", arContracts.account());
            assertEquals("onboarding", arContracts.pathPrefix());
            assertEquals(StorageAuthenticationType.MANAGED_IDENTITY, arContracts.authentication().type());
            assertEquals("stpnpgproducts", pnpgProducts.account());
            assertEquals("UseDevelopmentStorage=true", registry.storageConnectionString("AR", "products").orElseThrow());
            assertEquals("mi-contracts", registry.storageManagedIdentityClientId("AR", "contracts").orElseThrow());
            assertThrows(UnknownStorageException.class, () -> registry.storage("PNPG", "contracts"));
        } finally {
            System.clearProperty("MONGODB_CONNECTION_STRING_AR");
            System.clearProperty("MONGODB_CONNECTION_STRING_PNPG");
            System.clearProperty("BLOB_STORAGE_CONN_STRING_AR_PRODUCTS");
            System.clearProperty("BLOB_STORAGE_CONN_STRING_PNPG_PRODUCTS");
            System.clearProperty("AZURE_CLIENT_ID_AR_CONTRACTS");
        }
    }

    @Test
    void initialize_failsWhenMandatoryStorageIsMissing() {
        String json =
                "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcOnboarding\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"}}}";
        TenantRegistry registry = newRegistry(json, "AR", "products");
        System.setProperty("MONGODB_CONNECTION_STRING_AR", "mongodb://ar");
        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class, registry::initialize);
            assertTrue(exception.getMessage().contains("products"));
        } finally {
            System.clearProperty("MONGODB_CONNECTION_STRING_AR");
        }
    }

    @Test
    void initialize_failsWhenConnectionStringAndManagedIdentityAreCombined() {
        String json =
                "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcOnboarding\","
                        + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"},"
                        + "\"storages\":{\"products\":{\"account\":\"stselcarproducts\",\"container\":\"products\","
                        + "\"authentication\":{\"type\":\"CONNECTION_STRING\","
                        + "\"connectionStringEnvVar\":\"BLOB_STORAGE_CONN_STRING_AR_PRODUCTS\","
                        + "\"managedIdentityClientIdEnvVar\":\"AZURE_CLIENT_ID_AR_PRODUCTS\"}}}}}";
        TenantRegistry registry = newRegistry(json, "AR", "");
        System.setProperty("MONGODB_CONNECTION_STRING_AR", "mongodb://ar");
        System.setProperty("BLOB_STORAGE_CONN_STRING_AR_PRODUCTS", "UseDevelopmentStorage=true");
        System.setProperty("AZURE_CLIENT_ID_AR_PRODUCTS", "mi");
        try {
            assertThrows(IllegalStateException.class, registry::initialize);
        } finally {
            System.clearProperty("MONGODB_CONNECTION_STRING_AR");
            System.clearProperty("BLOB_STORAGE_CONN_STRING_AR_PRODUCTS");
            System.clearProperty("AZURE_CLIENT_ID_AR_PRODUCTS");
        }
    }

    private static TenantRegistry newRegistry(String json, String supportedTenants, String mandatoryStorageKeys) {
        TenantRegistry registry = new TenantRegistry();
        registry.tenantRegistryJson = json;
        registry.supportedTenants = supportedTenants;
        registry.mandatoryStorageKeys = Optional.of(mandatoryStorageKeys);
        return registry;
    }
}
