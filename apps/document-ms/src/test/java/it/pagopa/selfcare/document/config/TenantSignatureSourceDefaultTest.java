package it.pagopa.selfcare.document.config;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TenantSignatureSourceDefaultTest {

    @Inject TenantRegistry tenantRegistry;

    @Test
    void arSignatureIsDisabledWhenSourceVariableIsUnset() {
        assertThat(tenantRegistry.signatureCredentials("AR").orElseThrow().source()).isEqualTo("disabled");
    }
}
