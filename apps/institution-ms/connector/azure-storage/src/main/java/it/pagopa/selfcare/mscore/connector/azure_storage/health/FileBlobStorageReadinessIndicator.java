package it.pagopa.selfcare.mscore.connector.azure_storage.health;

import it.pagopa.selfcare.commons.health.spring.AbstractBlobStorageReadinessIndicator;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.mscore.connector.azure_storage.AzureBlobClient;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("fileBlobStorageReadiness")
public class FileBlobStorageReadinessIndicator extends AbstractBlobStorageReadinessIndicator {

    private final AzureBlobClient blobClient;
    private final TenantRegistry tenantRegistry;

    public FileBlobStorageReadinessIndicator(
            AzureBlobClient blobClient,
            TenantRegistry tenantRegistry) {
        this.blobClient = blobClient;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    protected String checkName() {
        return "blob-storage-files";
    }

    @Override
    protected String account() {
        return "tenant-specific";
    }

    @Override
    protected String container() {
        return "tenant-specific";
    }

    @Override
    protected void probe() {
        tenantRegistry.supportedTenantIds().forEach(tenantId -> {
            try {
                blobClient.probeContainer(tenantId);
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "Blob storage probe failed for tenant "
                                + tenantId + " and key mail-templates");
            }
        });
    }

    @Override
    protected Map<String, Object> data() {
        Map<String, Object> details = new HashMap<>(super.data());
        details.put("tenants", tenantRegistry.supportedTenantIds());
        details.put("storageKey", "mail-templates");
        return details;
    }
}
