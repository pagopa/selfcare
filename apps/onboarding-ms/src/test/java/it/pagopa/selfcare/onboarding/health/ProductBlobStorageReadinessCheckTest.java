package it.pagopa.selfcare.onboarding.health;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductBlobStorageReadinessCheckTest {

    private static final String ACCOUNT   = "account-name";
    private static final String CONTAINER = "product";
    private static final String PROBE_TARGET = "products.json";

    private AzureBlobClientDefault blobClient;
    private ProductBlobStorageReadinessCheck check;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClientDefault.class);
        check = new ProductBlobStorageReadinessCheck(blobClient, CONTAINER, Optional.of(ACCOUNT), PROBE_TARGET);
    }

    private HealthCheckResponse await() {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenProbeTargetIsReadable() {
        when(blobClient.getProperties(PROBE_TARGET)).thenReturn(null);

        HealthCheckResponse response = await();

        assertThat(response.getName()).isEqualTo("blob-storage-product");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isPresent();
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "blob-storage")
                .containsEntry("account",   ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_TARGET)
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenBlobClientFailsWithAuthorizationError() {
        // Simulates the failure mode where the Azure Blob client cannot read the probe target
        // (for example: HTTP 403 due to an RBAC/Managed Identity misconfiguration on the
        // storage account). The readiness probe MUST turn DOWN so that the pod is removed
        // from the load balancer
        when(blobClient.getProperties(PROBE_TARGET))
                .thenThrow(new RuntimeException("Status code 403, AuthorizationPermissionMismatch"));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("account",   ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_TARGET)
                .hasEntrySatisfying("error", err -> assertThat(err.toString())
                        .contains("403")
                        .contains("AuthorizationPermissionMismatch"));
    }

    @Test
    void down_whenClientThrowsRuntimeException() {
        when(blobClient.getProperties(PROBE_TARGET)).thenThrow(new RuntimeException("connection refused"));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("error", "RuntimeException: connection refused");
    }

    @Test
    void up_whenAccountNameIsNotConfigured_asInLocalConnectionStringMode() {
        // In local development the application authenticates against Azurite via connection
        // string, so onboarding-ms.blob-storage.account-name-product is not set. The readiness
        // check must still work: account is only informational metadata.
        ProductBlobStorageReadinessCheck localCheck =
                new ProductBlobStorageReadinessCheck(blobClient, CONTAINER, Optional.empty(), PROBE_TARGET);
        when(blobClient.getProperties(PROBE_TARGET)).thenReturn(null);

        HealthCheckResponse response = localCheck.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData().orElseThrow())
                .containsEntry("account",   "n/a")
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_TARGET);
    }
}
