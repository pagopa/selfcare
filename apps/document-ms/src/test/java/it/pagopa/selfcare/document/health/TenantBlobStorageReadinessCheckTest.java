package it.pagopa.selfcare.document.health;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.storage.StorageKeys;
import it.pagopa.selfcare.document.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantBlobStorageReadinessCheckTest {

    private static final String PROBE_PREFIX = TenantBlobStorageReadinessCheck.READINESS_PROBE_PREFIX;

    @Test
    void up_whenEveryTenantMandatoryStorageProbeSucceeds() {
        AzureBlobClient arContracts = client(List.of());
        AzureBlobClient arUser = client(List.of());
        AzureBlobClient pnpgContracts = client(List.of());
        AzureBlobClient pnpgUser = client(List.of());
        TenantBlobClientProvider provider = mock(TenantBlobClientProvider.class);
        when(provider.clientFor("AR", StorageKeys.CONTRACTS)).thenReturn(arContracts);
        when(provider.clientFor("AR", StorageKeys.USER_ATTACHMENTS)).thenReturn(arUser);
        when(provider.clientFor("PNPG", StorageKeys.CONTRACTS)).thenReturn(pnpgContracts);
        when(provider.clientFor("PNPG", StorageKeys.USER_ATTACHMENTS)).thenReturn(pnpgUser);

        TenantBlobStorageReadinessCheck check = new TenantBlobStorageReadinessCheck(registry(Set.of("AR", "PNPG")), provider);

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getName()).isEqualTo("blob-storage-document");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData().orElseThrow())
                .containsEntry("component", "blob-storage")
                .containsEntry("account", "AR:contracts=ar-docs,AR:user-attachments=ar-user,PNPG:contracts=pnpg-docs,PNPG:user-attachments=pnpg-user")
                .containsEntry("container", "AR:contracts=ar-documents,AR:user-attachments=ar-user-attachments,PNPG:contracts=pnpg-documents,PNPG:user-attachments=pnpg-user-attachments")
                .containsEntry("probeTarget", PROBE_PREFIX)
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenAnyTenantMandatoryStorageProbeFails() {
        AzureBlobClient arContracts = client(List.of());
        AzureBlobClient arUser = mock(AzureBlobClient.class);
        when(arUser.getFiles(PROBE_PREFIX)).thenThrow(new RuntimeException("Status code 403"));
        TenantBlobClientProvider provider = mock(TenantBlobClientProvider.class);
        when(provider.clientFor("AR", StorageKeys.CONTRACTS)).thenReturn(arContracts);
        when(provider.clientFor("AR", StorageKeys.USER_ATTACHMENTS)).thenReturn(arUser);

        TenantBlobStorageReadinessCheck check = new TenantBlobStorageReadinessCheck(registry(Set.of("AR")), provider);

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().orElseThrow())
                .hasEntrySatisfying("error", error -> assertThat(error.toString())
                        .contains("Tenant AR storage user-attachments probe failed")
                        .contains("403"));
    }

    @Test
    void down_whenNoMandatoryStorageIsConfigured() {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.supportedTenantIds()).thenReturn(Set.of("AR"));
        when(registry.mandatoryStorageKeys()).thenReturn(Set.of());

        TenantBlobStorageReadinessCheck check = new TenantBlobStorageReadinessCheck(registry, mock(TenantBlobClientProvider.class));

        HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(5));

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().orElseThrow())
                .containsEntry("account", "n/a")
                .containsEntry("container", "n/a")
                .hasEntrySatisfying("error", error -> assertThat(error.toString()).contains("No mandatory tenant storage configured"));
    }

    private AzureBlobClient client(List<String> result) {
        AzureBlobClient client = mock(AzureBlobClient.class);
        when(client.getFiles(PROBE_PREFIX)).thenReturn(result);
        return client;
    }

    private TenantRegistry registry(Set<String> tenants) {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.supportedTenantIds()).thenReturn(tenants);
        when(registry.mandatoryStorageKeys()).thenReturn(Set.of(StorageKeys.CONTRACTS, StorageKeys.USER_ATTACHMENTS));
        tenants.forEach(tenant -> {
            when(registry.storage(tenant, StorageKeys.CONTRACTS)).thenReturn(storage(tenant.toLowerCase() + "-docs", tenant.toLowerCase() + "-documents"));
            when(registry.storage(tenant, StorageKeys.USER_ATTACHMENTS)).thenReturn(storage(tenant.toLowerCase() + "-user", tenant.toLowerCase() + "-user-attachments"));
        });
        return registry;
    }

    private TenantDefinition.StorageDefinition storage(String account, String container) {
        return new TenantDefinition.StorageDefinition(
                account,
                container,
                "",
                new TenantDefinition.StorageAuthentication(StorageAuthenticationType.MANAGED_IDENTITY, "AZURE_CLIENT_ID_AR_DOCUMENTS", null));
    }
}
