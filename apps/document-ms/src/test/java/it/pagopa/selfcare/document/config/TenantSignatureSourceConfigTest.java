package it.pagopa.selfcare.document.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The AR registry entry keeps the per-environment PAGOPA_SIGNATURE_SOURCE value so that production
 * (where signing is disabled) does not start signing with the Step 2 rollout.
 */
@QuarkusTest
@TestProfile(TenantSignatureSourceConfigTest.NamirialSourceProfile.class)
class TenantSignatureSourceConfigTest {

    @Inject TenantRegistry tenantRegistry;

    @Test
    void arSignatureSourceFollowsEnvironmentVariable() {
        TenantRegistry.SignatureCredentials signature =
                tenantRegistry.signatureCredentials("AR").orElseThrow();

        assertThat(signature.source()).isEqualTo("namirial");
        assertThat(signature.signer()).isEqualTo("PagoPA S.p.A.");
        assertThat(signature.namirial()).isPresent();
        assertThat(signature.namirial().get().baseUrl()).isEqualTo("https://namirial.example.test");
    }

    public static class NamirialSourceProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("PAGOPA_SIGNATURE_SOURCE", "namirial");
        }
    }
}
