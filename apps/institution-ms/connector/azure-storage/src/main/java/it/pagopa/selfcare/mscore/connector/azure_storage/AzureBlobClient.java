package it.pagopa.selfcare.mscore.connector.azure_storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import it.pagopa.selfcare.mscore.api.FileStorageConnector;
import it.pagopa.selfcare.mscore.config.AzureStorageConfig;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.pagopa.selfcare.mscore.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;

@Slf4j
@Service
@PropertySource("classpath:config/azure-storage-config.properties")
@Profile("AzureStorage")
public class AzureBlobClient implements FileStorageConnector {

    private final BlobServiceClient blobClient;
    private final AzureStorageConfig azureStorageConfig;

    AzureBlobClient(AzureStorageConfig azureStorageConfig) {
        log.trace("AzureBlobClient.AzureBlobClient");
        this.azureStorageConfig = azureStorageConfig;
        this.blobClient = Optional.ofNullable(azureStorageConfig.getConnectionString())
            .filter(cs -> !cs.isBlank())
            .map(cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient())
            .orElseGet(() -> new BlobServiceClientBuilder()
                .endpoint("https://" + azureStorageConfig.getAccountName() + ".blob.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(azureStorageConfig.getManagedIdentityClientId())
                    .build()
                ).buildClient());
    }

    @Override
    public String getTemplateFile(String templateName) {
        log.info("START - getTemplateFile for template: {}", templateName);
        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(azureStorageConfig.getContainer());
            final BlobClient blob = blobContainer.getBlobClient(templateName);
            String downloaded = blob.downloadContent().toString();
            log.info("END - getTemplateFile - Downloaded {}", templateName);
            return downloaded;
        } catch (Exception e) {
            log.error(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName), e);
            throw new MsCoreException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

}
