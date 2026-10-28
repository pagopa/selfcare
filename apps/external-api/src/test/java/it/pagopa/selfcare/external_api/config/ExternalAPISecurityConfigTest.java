package it.pagopa.selfcare.external_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalAPISecurityConfigTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"AR\":{}}",
            "{\"AR\":{\"jwt\":{\"publicKeyEnvVar\":\"KEY\",\"issuers\":[\"SPID\"]}}}",
            "{\"AR\":{\"jwt\":{\"publicKeyEnvVar\":\"KEY\",\"audiences\":[\"audience\"]}}}"
    })
    void rejectsMissingTenantTrustConfiguration(String json) {
        MockEnvironment environment = environment();
        TenantRegistry registry = new TenantRegistry(new ObjectMapper(), environment, json, "*", "");
        registry.initialize();
        assertThrows(IllegalStateException.class, () -> new ExternalAPISecurityConfig(registry, environment));
    }

    @Test
    void rejectsSharedDeploymentWithoutMandatoryTenantClaims() {
        MockEnvironment environment = environment().withProperty("jwt.tenant-claim-required", "false");
        TenantRegistry registry = new TenantRegistry(new ObjectMapper(), environment, """
                {"AR":{"jwt":{"publicKeyEnvVar":"KEY","issuers":["SPID"],"audiences":["audience"]}},
                 "PNPG":{"jwt":{"publicKeyEnvVar":"KEY","issuers":["SPID"],"audiences":["audience"]}}}
                """, "AR,PNPG", "");
        registry.initialize();
        assertThrows(IllegalStateException.class, () -> new ExternalAPISecurityConfig(registry, environment));
    }

    private static MockEnvironment environment() {
        return new MockEnvironment().withProperty("tenant.mongodb.required", "false")
                .withProperty("KEY", "test-public-key");
    }
}
