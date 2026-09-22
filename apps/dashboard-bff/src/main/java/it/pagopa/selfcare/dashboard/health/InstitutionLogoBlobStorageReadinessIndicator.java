package it.pagopa.selfcare.dashboard.health;

import it.pagopa.selfcare.commons.health.spring.AbstractBlobStorageReadinessIndicator;
import it.pagopa.selfcare.dashboard.client.azure_storage.AzureBlobClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("institutionLogoBlobStorageReadiness")
@Profile("AzureStorage")
public class InstitutionLogoBlobStorageReadinessIndicator
        extends AbstractBlobStorageReadinessIndicator {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClient blobClient;
    private final String container;
    private final String account;

    public InstitutionLogoBlobStorageReadinessIndicator(
            AzureBlobClient blobClient,
            @Value("${blobStorage.institutions.logo.containerReference}") String container,
            @Value("${blobStorage.accountNameProduct}") String account) {
        this.blobClient = blobClient;
        this.container = container;
        this.account = account == null || account.isBlank() ? ACCOUNT_NOT_APPLICABLE : account;
    }

    @Override
    protected String checkName() {
        return "blob-storage-institution-logo";
    }

    @Override
    protected String account() {
        return account;
    }

    @Override
    protected String container() {
        return container;
    }

    @Override
    protected void probe() {
        blobClient.probeContainer();
    }
}
