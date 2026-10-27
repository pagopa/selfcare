package it.pagopa.selfcare.mscore.core.health;

import it.pagopa.selfcare.commons.health.spring.AbstractBlobStorageReadinessIndicator;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component("productBlobStorageReadiness")
public class ProductBlobStorageReadinessIndicator extends AbstractBlobStorageReadinessIndicator {

    private static final String PRODUCTS_STORAGE_KEY = "products";

    private final TenantBlobClientProvider blobClientProvider;
    private final TenantRegistry tenantRegistry;
    private final String productFilePath;

    public ProductBlobStorageReadinessIndicator(
            TenantBlobClientProvider blobClientProvider,
            TenantRegistry tenantRegistry,
            @Value("${mscore.blob-storage.filepath-product}") String productFilePath) {
        this.blobClientProvider = blobClientProvider;
        this.tenantRegistry = tenantRegistry;
        this.productFilePath = productFilePath;
    }

    @Override
    protected String checkName() {
        return "blob-storage-product";
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
    protected String probeTarget() {
        return productFilePath;
    }

    @Override
    protected void probe() {
        tenantRegistry.supportedTenantIds().forEach(tenantId -> {
            try {
                blobClientProvider.clientFor(tenantId, PRODUCTS_STORAGE_KEY)
                        .blob(productFilePath)
                        .getProperties();
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "Blob storage probe failed for tenant "
                                + tenantId + " and key products");
            }
        });
    }

    @Override
    protected Map<String, Object> data() {
        Map<String, Object> details = new HashMap<>(super.data());
        details.put("tenants", tenantRegistry.supportedTenantIds());
        details.put("storageKey", PRODUCTS_STORAGE_KEY);
        return details;
    }
}
