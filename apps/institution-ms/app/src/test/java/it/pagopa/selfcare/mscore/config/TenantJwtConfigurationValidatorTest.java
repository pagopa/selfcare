package it.pagopa.selfcare.mscore.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

class TenantJwtConfigurationValidatorTest {

    @Test
    void rejectsSupportedTenantWithoutJwtDefinition() {
        TenantRegistry registry = registry(
                new MockEnvironment().withProperty("MONGO_AR", "mongodb://ar:27017"),
                """
                {"AR":{"mongo":{"account":"ar","database":"db","connectionStringEnvVar":"MONGO_AR"}}}
                """);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new TenantJwtConfigurationValidator(registry).validateTenantKeys());

        assertFalse(exception.getMessage().contains("mongodb://"));
        assertStartupFails(registry);
    }

    @Test
    void rejectsMissingJwtValueDuringRegistryInitializationWithoutExposingValue() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://ar:27017")
                .withProperty("JWT_PUBLIC_KEY_AR", "");
        String registryJson = """
                {"AR":{
                  "mongo":{"account":"ar","database":"db","connectionStringEnvVar":"MONGO_AR"},
                  "jwt":{"publicKeyEnvVar":"JWT_PUBLIC_KEY_AR"}
                }}
                """;
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registry(environment, registryJson));

        assertFalse(exception.getMessage().contains("mongodb://ar:27017"));
        assertStartupFails(uninitializedRegistry(
                new MockEnvironment()
                        .withProperty("MONGO_AR", "mongodb://ar:27017")
                        .withProperty("JWT_PUBLIC_KEY_AR", ""),
                registryJson));
    }

    @Test
    void acceptsTenantWithResolvableJwtValue() {
        TenantRegistry registry = registry(
                new MockEnvironment()
                        .withProperty("MONGO_AR", "mongodb://ar:27017")
                        .withProperty("JWT_PUBLIC_KEY_AR", "configured-key"),
                """
                {"AR":{
                  "mongo":{"account":"ar","database":"db","connectionStringEnvVar":"MONGO_AR"},
                  "jwt":{"publicKeyEnvVar":"JWT_PUBLIC_KEY_AR"}
                }}
                """);

        assertDoesNotThrow(() -> new TenantJwtConfigurationValidator(registry).validateTenantKeys());
    }

    private TenantRegistry registry(MockEnvironment environment, String registryJson) {
        TenantRegistry registry = uninitializedRegistry(environment, registryJson);
        registry.initialize();
        return registry;
    }

    private TenantRegistry uninitializedRegistry(
            MockEnvironment environment, String registryJson) {
        return new TenantRegistry(new ObjectMapper(), environment, registryJson, "AR", "");
    }

    private void assertStartupFails(TenantRegistry registry) {
        new ApplicationContextRunner()
                .withBean(TenantRegistry.class, () -> registry)
                .withBean(TenantJwtConfigurationValidator.class)
                .run(context -> {
                    org.junit.jupiter.api.Assertions.assertNotNull(context.getStartupFailure());
                    org.junit.jupiter.api.Assertions.assertFalse(
                            context.getStartupFailure().toString().contains("mongodb://"));
                });
    }
}
