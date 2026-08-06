package it.pagopa.selfcare.product.health;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobContainerProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductBlobStorageReadinessCheckTest {

    private static final String CONTAINER = "sc-d-documents-blob";
    private static final String ACCOUNT   = "n/a";

    private BlobContainerAsyncClient containerClient;
    private ProductBlobStorageReadinessCheck check;

    @BeforeEach
    void setUp() {
        BlobServiceAsyncClient blobClient = mock(BlobServiceAsyncClient.class);
        containerClient = mock(BlobContainerAsyncClient.class);
        when(blobClient.getBlobContainerAsyncClient(CONTAINER)).thenReturn(containerClient);
        check = new ProductBlobStorageReadinessCheck(CONTAINER, blobClient, ACCOUNT);
    }

    private HealthCheckResponse await() {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenContainerGetPropertiesSucceeds() {
        when(containerClient.getProperties())
                .thenReturn(Mono.just(mock(BlobContainerProperties.class)));

        HealthCheckResponse response = await();

        assertThat(response.getName()).isEqualTo("blob-storage-contract-template");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "blob-storage")
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsKey("latencyMs")
                .doesNotContainKey("error")
                .doesNotContainKey("probeTarget");
    }

    @Test
    void down_whenBlobClientFailsWithAuthorizationError() {
        when(containerClient.getProperties())
                .thenReturn(Mono.error(new RuntimeException("Status code 403, AuthorizationPermissionMismatch")));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .hasEntrySatisfying("error", err -> assertThat(err.toString())
                        .contains("403")
                        .contains("AuthorizationPermissionMismatch"));
    }

    @Test
    void down_whenClientFailsWithGenericException() {
        when(containerClient.getProperties())
                .thenReturn(Mono.error(new RuntimeException("connection refused")));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().orElseThrow())
                .containsEntry("error", "RuntimeException: connection refused");
    }

    @Test
    void productionConstructor_buildsBeanWithoutErrors() {
        ProductBlobStorageReadinessCheck prodCheck = new ProductBlobStorageReadinessCheck(
                CONTAINER, "UseDevelopmentStorage=true;");

        assertThat(prodCheck).isNotNull();
    }
}


