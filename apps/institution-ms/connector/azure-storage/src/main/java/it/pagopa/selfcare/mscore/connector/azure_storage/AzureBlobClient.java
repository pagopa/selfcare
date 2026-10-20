package it.pagopa.selfcare.mscore.connector.azure_storage;

import com.azure.storage.blob.BlobClient;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClient;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.mscore.api.FileStorageConnector;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.pagopa.selfcare.mscore.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;

@Slf4j
@Service
public class AzureBlobClient implements FileStorageConnector {

    private static final String MAIL_TEMPLATES_STORAGE_KEY = "mail-templates";

    private final TenantBlobClientProvider blobClientProvider;
    AzureBlobClient(TenantBlobClientProvider blobClientProvider) {
        log.trace("AzureBlobClient.AzureBlobClient");
        this.blobClientProvider = blobClientProvider;
    }

    @Override
    public String getTemplateFile(String templateName) {
        log.info("START - getTemplateFile for template: {}", templateName);
        try {
            TenantBlobClient storage =
                    blobClientProvider.clientForCurrentTenant(MAIL_TEMPLATES_STORAGE_KEY);
            final BlobClient blob = storage.blob(templateName);
            String downloaded = blob.downloadContent().toString();
            log.info("END - getTemplateFile - Downloaded {}", templateName);
            return downloaded;
        } catch (Exception e) {
            log.error(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName), e);
            throw new MsCoreException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

    public void probeContainer() {
        blobClientProvider.clientForCurrentTenant(MAIL_TEMPLATES_STORAGE_KEY)
                .blob("").getContainerClient().getProperties();
    }

    public void probeContainer(String tenantId) {
        blobClientProvider.clientFor(tenantId, MAIL_TEMPLATES_STORAGE_KEY)
                .blob("").getContainerClient().getProperties();
    }

}
