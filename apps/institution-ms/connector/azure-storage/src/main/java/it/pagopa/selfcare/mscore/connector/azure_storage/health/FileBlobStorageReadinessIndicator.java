package it.pagopa.selfcare.mscore.connector.azure_storage.health;

import it.pagopa.selfcare.commons.health.spring.AbstractBlobStorageReadinessIndicator;
import it.pagopa.selfcare.mscore.config.AzureStorageConfig;
import it.pagopa.selfcare.mscore.connector.azure_storage.AzureBlobClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("fileBlobStorageReadiness")
@Profile("AzureStorage")
public class FileBlobStorageReadinessIndicator extends AbstractBlobStorageReadinessIndicator {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClient blobClient;
    private final AzureStorageConfig config;

    public FileBlobStorageReadinessIndicator(
            AzureBlobClient blobClient,
            AzureStorageConfig config) {
        this.blobClient = blobClient;
        this.config = config;
    }

    @Override
    protected String checkName() {
        return "blob-storage-files";
    }

    @Override
    protected String account() {
        String account = config.getAccountName();
        return account == null || account.isBlank() ? ACCOUNT_NOT_APPLICABLE : account;
    }

    @Override
    protected String container() {
        return config.getContainer();
    }

    @Override
    protected void probe() {
        blobClient.probeContainer();
    }
}
