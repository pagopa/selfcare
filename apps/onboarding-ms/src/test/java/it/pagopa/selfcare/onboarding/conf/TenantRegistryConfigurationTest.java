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
    void defaultRegistryPreservesBothTenantsWithoutCatalogStorage() throws IOException {
        SmallRyeConfig config = configuration(Map.of());

        JsonNode registry = new ObjectMapper().readTree(config.getValue("tenant.registry.json", String.class));

        assertEquals(2, registry.size());
        assertTrue(registry.has("AR"));
        assertTrue(registry.has("PNPG"));
        for (JsonNode tenant : registry) {
            assertEquals("selcOnboarding", tenant.path("mongo").path("database").asText());
            assertFalse(tenant.has("storages"));
        }
    }

    @Test
    void environmentOverridesRegistryWithoutExpandingNestedJson() throws IOException {
        String registryJson = """
                {"PNPG":{"mongo":{"account":"local","database":"testOnboarding","connectionStringEnvVar":"TEST_MONGO"}}}
                """.trim();

        SmallRyeConfig config = configuration(Map.of("TENANT_REGISTRY_JSON", registryJson));

        assertEquals(registryJson, config.getValue("tenant.registry.json", String.class));
    }

    private SmallRyeConfig configuration(Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        }
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "main-application"),
                        new EnvConfigSource(environment, 300))
                .addDefaultInterceptors()
                .build();
    }
}
