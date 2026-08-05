package it.pagopa.selfcare.document.health;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.config.StorageRegistry;
import it.pagopa.selfcare.document.model.StorageOrigin;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ContractsBlobStorageReadinessCheckTest {

    private static final String ACCOUNT = "account-name";
    private static final String CONTAINER = "sc-d-documents-blob";
    private static final String PROBE_PREFIX = ContractsBlobStorageReadinessCheck.READINESS_PROBE_PREFIX;

    private AzureBlobClient blobClient;
    private ContractsBlobStorageReadinessCheck check;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        StorageRegistry registry = mock(StorageRegistry.class);
        when(registry.clientFor(StorageOrigin.SYSTEM)).thenReturn(blobClient);
        check = new ContractsBlobStorageReadinessCheck(registry, CONTAINER, Optional.of(ACCOUNT));
    }

    private HealthCheckResponse await() {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenListWithProbePrefixSucceeds() {
        when(blobClient.getFiles(PROBE_PREFIX)).thenReturn(List.of());

        HealthCheckResponse response = await();

        assertThat(response.getName()).isEqualTo("blob-storage-contracts");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "blob-storage")
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("canaryBlob", PROBE_PREFIX)
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenBlobClientFailsWithAuthorizationError() {
        when(blobClient.getFiles(PROBE_PREFIX))
                .thenThrow(new RuntimeException("Status code 403, AuthorizationPermissionMismatch"));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("canaryBlob", PROBE_PREFIX)
                .hasEntrySatisfying("error", err -> assertThat(err.toString())
                        .contains("403")
                        .contains("AuthorizationPermissionMismatch"));
    }

    @Test
    void down_whenClientThrowsRuntimeException() {
        when(blobClient.getFiles(PROBE_PREFIX)).thenThrow(new RuntimeException("connection refused"));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data).containsEntry("error", "RuntimeException: connection refused");
    }

    @Test
    void up_whenAccountNameIsNotConfigured_asInLocalConnectionStringMode() {
        StorageRegistry registry = mock(StorageRegistry.class);
        when(registry.clientFor(StorageOrigin.SYSTEM)).thenReturn(blobClient);
        ContractsBlobStorageReadinessCheck localCheck =
                new ContractsBlobStorageReadinessCheck(registry, CONTAINER, Optional.empty());
        when(blobClient.getFiles(PROBE_PREFIX)).thenReturn(List.of());

        HealthCheckResponse response = localCheck.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData().orElseThrow())
                .containsEntry("account", "n/a")
                .containsEntry("container", CONTAINER)
                .containsEntry("canaryBlob", PROBE_PREFIX);
    }
}
