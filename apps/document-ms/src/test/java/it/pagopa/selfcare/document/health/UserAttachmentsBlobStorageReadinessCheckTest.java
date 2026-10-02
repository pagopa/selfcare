package it.pagopa.selfcare.document.health;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.storage.StorageKeys;
import it.pagopa.selfcare.document.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAttachmentsBlobStorageReadinessCheckTest {

    private static final String TENANT_ID = "AR";
    private static final String ACCOUNT = "account-name";
    private static final String CONTAINER = "sc-d-usrattach-blob";
    private static final String PROBE_PREFIX = UserAttachmentsBlobStorageReadinessCheck.READINESS_PROBE_PREFIX;

    private AzureBlobClient blobClient;
    private UserAttachmentsBlobStorageReadinessCheck check;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        check = new UserAttachmentsBlobStorageReadinessCheck(provider(StorageKeys.USER_ATTACHMENTS), registry(StorageKeys.USER_ATTACHMENTS, storage(ACCOUNT)));
    }

    @Test
    void up_whenListWithProbePrefixSucceeds() {
        when(blobClient.getFiles(PROBE_PREFIX)).thenReturn(List.of());

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getName()).isEqualTo("blob-storage-user-attachments");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "blob-storage")
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_PREFIX)
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenBlobClientFailsWithAuthorizationError() {
        when(blobClient.getFiles(PROBE_PREFIX))
                .thenThrow(new RuntimeException("Status code 403, AuthorizationPermissionMismatch"));

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("account", ACCOUNT)
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_PREFIX)
                .hasEntrySatisfying("error", err -> assertThat(err.toString())
                        .contains("403")
                        .contains("AuthorizationPermissionMismatch"));
    }

    @Test
    void down_whenClientThrowsRuntimeException() {
        when(blobClient.getFiles(PROBE_PREFIX)).thenThrow(new RuntimeException("connection refused"));

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().orElseThrow())
                .containsEntry("error", "RuntimeException: connection refused");
    }

    @Test
    void up_whenAccountNameIsNotConfigured_asInLocalConnectionStringMode() {
        UserAttachmentsBlobStorageReadinessCheck localCheck =
                new UserAttachmentsBlobStorageReadinessCheck(provider(StorageKeys.USER_ATTACHMENTS), registry(StorageKeys.USER_ATTACHMENTS, storage(null)));
        when(blobClient.getFiles(PROBE_PREFIX)).thenReturn(List.of());

        HealthCheckResponse response = localCheck.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData().orElseThrow())
                .containsEntry("account", "n/a")
                .containsEntry("container", CONTAINER)
                .containsEntry("probeTarget", PROBE_PREFIX);
    }

    private TenantBlobClientProvider provider(String key) {
        TenantBlobClientProvider provider = mock(TenantBlobClientProvider.class);
        when(provider.clientFor(TENANT_ID, key)).thenReturn(blobClient);
        return provider;
    }

    private TenantRegistry registry(String key, TenantDefinition.StorageDefinition storage) {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.supportedTenantIds()).thenReturn(Set.of(TENANT_ID));
        when(registry.storage(TENANT_ID, key)).thenReturn(storage);
        return registry;
    }

    private TenantDefinition.StorageDefinition storage(String account) {
        return new TenantDefinition.StorageDefinition(
                account,
                CONTAINER,
                "",
                new TenantDefinition.StorageAuthentication(StorageAuthenticationType.MANAGED_IDENTITY, "AZURE_CLIENT_ID_AR_DOCUMENTS", null));
    }
}
