package it.pagopa.selfcare.onboarding.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The Spring reference uses Feign defaults even when its legacy timeout environment variables are set.
 */
class RestClientTimeoutDefaultsTest {

    private static final Path MAIN_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final List<String> CLIENTS = List.of("iam_json", "institution_json", "onboarding_json",
            "user_json", "party_process", "party_registry_proxy", "user_registry_json");
    private static final List<String> AGGREGATES_CLIENTS = List.of(
            "\"org.openapi.quarkus.onboarding_json.api.AggregatesControllerApi\"",
            "\"org.openapi.quarkus.onboarding_json.api.InstitutionControllerApi\"");

    @Test
    void withoutEnvironment_defaultsAreTheEffectiveFeignOnesNotTheUnenforcedSpringFiveSeconds() throws IOException {
        assertFeignDefaults(config(Map.of()));
    }

    private static void assertFeignDefaults(SmallRyeConfig config) {
        for (String client : CLIENTS) {
            assertEquals(10000L, timeout(config, client, "connect-timeout"), client);
            assertEquals(60000L, timeout(config, client, "read-timeout"), client);
        }
        for (String client : List.of("product_json", "document_json", "onboarding_functions_json")) {
            assertEquals(10000L, timeout(config, client, "connect-timeout"), client);
            assertEquals(60000L, timeout(config, client, "read-timeout"), client);
        }
        for (String client : AGGREGATES_CLIENTS) {
            assertEquals(10000L, timeout(config, client, "connect-timeout"), client);
            assertEquals(60000L, timeout(config, client, "read-timeout"), client);
        }
    }

    @Test
    void legacyGlobalEnvironment_doesNotChangeTheEffectiveSpringTimeouts() throws IOException {
        SmallRyeConfig config = config(Map.of("REST_CLIENT_CONNECT_TIMEOUT", "1234", "REST_CLIENT_READ_TIMEOUT", "4321"));

        assertFeignDefaults(config);
    }

    @Test
    void legacyPerServiceEnvironment_doesNotChangeTheEffectiveSpringTimeouts() throws IOException {
        SmallRyeConfig config = config(Map.of(
                "REST_CLIENT_READ_TIMEOUT", "4321",
                "IAM_REST_CLIENT_READ_TIMEOUT", "11",
                "IAM_REST_CLIENT_CONNECT_TIMEOUT", "12",
                "USERVICE_USER_REGISTRY_REST_CLIENT_READ_TIMEOUT", "22",
                "USERVICE_USER_REGISTRY_REST_CLIENT_CONNECT_TIMEOUT", "23",
                "USERVICE_MS_CORE_REST_CLIENT_READ_TIMEOUT", "33",
                "USERVICE_MS_CORE_REST_CLIENT_CONNECT_TIMEOUT", "34",
                "USERVICE_PARTY_PROCESS_REST_CLIENT_READ_TIMEOUT", "44",
                "AGGREGATES_REST_CLIENT_READ_TIMEOUT", "55",
                "AGGREGATES_REST_CLIENT_CONNECT_TIMEOUT", "56"));

        assertFeignDefaults(config);
    }

    @Test
    void nativeQuarkusOverridesRemainAvailableForControlledTests() throws IOException {
        SmallRyeConfig config = config(Map.of("quarkus.rest-client.iam_json.read-timeout", "1500"));

        assertEquals(1500L, timeout(config, "iam_json", "read-timeout"));
        assertEquals(60000L, timeout(config, "onboarding_json", "read-timeout"));
    }

    private static SmallRyeConfig config(Map<String, String> environment) throws IOException {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new PropertiesConfigSource(MAIN_PROPERTIES.toUri().toURL(), 100))
                .withSources(new PropertiesConfigSource(environment, "environment", 300))
                .build();
    }

    private static long timeout(SmallRyeConfig config, String client, String name) {
        return config.getValue("quarkus.rest-client." + client + "." + name, Long.class);
    }
}
