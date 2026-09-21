package it.pagopa.selfcare.dashboard.health;

import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.commons.health.spring.AbstractBlobStorageReadinessIndicator;
import it.pagopa.selfcare.dashboard.config.DashboardConfig;
import org.springframework.stereotype.Component;

@Component("productBlobStorageReadiness")
public class ProductBlobStorageReadinessIndicator extends AbstractBlobStorageReadinessIndicator {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClientDefault blobClient;
    private final DashboardConfig.BlobStorage config;

    public ProductBlobStorageReadinessIndicator(
            AzureBlobClientDefault productBlobClient,
            DashboardConfig dashboardConfig) {
        this.blobClient = productBlobClient;
        this.config = dashboardConfig.getBlobStorage();
    }

    @Override
    protected String checkName() {
        return "blob-storage-product";
    }

    @Override
    protected String account() {
        String account = config.getAccountNameProduct();
        return account == null || account.isBlank() ? ACCOUNT_NOT_APPLICABLE : account;
    }

    @Override
    protected String container() {
        return config.getContainerProduct();
    }

    @Override
    protected String probeTarget() {
        return config.getFilepathProduct();
    }

    @Override
    protected void probe() {
        blobClient.getProperties(probeTarget());
    }
}
