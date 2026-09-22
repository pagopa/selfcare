package it.pagopa.selfcare.onboarding.health;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.storage.StorageKeys;
import it.pagopa.selfcare.onboarding.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductBlobStorageReadinessCheckTest {

    private static final String ACCOUNT = "account-name";
    private static final String CONTAINER = "product";
    private static final String PROBE_TARGET = "products.json";

    private AzureBlobClient blobClient;
    private TenantRegistry tenantRegistry;
    private TenantBlobClientProvider blobClientProvider;
    private ProductBlobStorageReadinessCheck check;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        tenantRegistry = mock(TenantRegistry.class);
        blobClientProvider = mock(TenantBlobClientProvider.class);
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR"));
        when(tenantRegistry.storage("AR", StorageKeys.PRODUCTS)).thenReturn(new TenantDefinition.StorageDefinition(
                ACCOUNT,
                CONTAINER,
                "",
                new TenantDefinition.StorageAuthentication(StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR")));
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(blobClient);
        check = new ProductBlobStorageReadinessCheck(tenantRegistry, blobClientProvider, PROBE_TARGET);
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
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_TARGET)
                .containsEntry("logicalKey", StorageKeys.PRODUCTS)
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenBlobClientFailsWithAuthorizationError() {
        when(blobClient.getProperties(PROBE_TARGET))
                .thenThrow(new RuntimeException("Status code 403, AuthorizationPermissionMismatch"));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("account", ACCOUNT)
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
}
