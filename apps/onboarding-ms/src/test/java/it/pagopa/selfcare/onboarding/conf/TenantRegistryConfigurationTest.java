package it.pagopa.selfcare.onboarding.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class TenantRegistryConfigurationTest {

    @Test
    void defaultRegistryPreservesBothTenantsWithLegacyStorageConfiguration() throws IOException {
        SmallRyeConfig config = configuration("main", Map.of());

        JsonNode registry = new ObjectMapper().readTree(config.getValue("tenant.registry.json", String.class));

        assertEquals(2, registry.size());
        assertTrue(registry.has("AR"));
        assertTrue(registry.has("PNPG"));
        assertEquals("products", config.getValue("tenant.storage.mandatory-keys", String.class));
        for (JsonNode tenant : registry) {
            assertEquals("selcOnboarding", tenant.path("mongo").path("database").asText());
            JsonNode storage = tenant.path("storages").path("products");
            assertFalse(storage.path("account").asText().isBlank());
            assertFalse(storage.path("container").asText().isBlank());
            assertEquals("CONNECTION_STRING", storage.path("authentication").path("type").asText());
        }
        assertEquals("BLOB_STORAGE_AR_PRODUCT_CONNECTION_STRING",
                registry.at("/AR/storages/products/authentication/connectionStringEnvVar").asText());
        assertEquals("BLOB_STORAGE_PNPG_PRODUCT_CONNECTION_STRING",
                registry.at("/PNPG/storages/products/authentication/connectionStringEnvVar").asText());
    }

    @Test
    void testRegistryProvidesLegacyStorageConfigurationForBothTenants() throws IOException {
        SmallRyeConfig config = configuration("test", Map.of());

        JsonNode registry = new ObjectMapper().readTree(config.getValue("tenant.registry.json", String.class));

        assertEquals(2, registry.size());
        assertTrue(registry.has("AR"));
        assertTrue(registry.has("PNPG"));
        assertEquals("products", config.getValue("tenant.storage.mandatory-keys", String.class));
        for (JsonNode tenant : registry) {
            assertEquals("dummyOnboarding", tenant.path("mongo").path("database").asText());
            JsonNode storage = tenant.path("storages").path("products");
            assertEquals("devstoreaccount1", storage.path("account").asText());
            assertEquals("products", storage.path("container").asText());
            assertEquals("CONNECTION_STRING", storage.path("authentication").path("type").asText());
            assertEquals("UseDevelopmentStorage=true", config.getValue(
                    storage.path("authentication").path("connectionStringEnvVar").asText(), String.class));
        }
    }

    @Test
    void environmentOverridesRegistryWithoutExpandingNestedJson() throws IOException {
        String registryJson = """
                {"PNPG":{"mongo":{"account":"local","database":"testOnboarding","connectionStringEnvVar":"TEST_MONGO"}}}
                """.trim();

        SmallRyeConfig config = configuration("main", Map.of("TENANT_REGISTRY_JSON", registryJson));

        assertEquals(registryJson, config.getValue("tenant.registry.json", String.class));
    }

    private SmallRyeConfig configuration(String sourceSet, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("src", sourceSet, "resources", "application.properties"))) {
            properties.load(input);
        }
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "main-application"),
                        new EnvConfigSource(environment, 300))
                .addDefaultInterceptors()
                .build();
    }
}
