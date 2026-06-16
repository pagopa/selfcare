package it.pagopa.selfcare.mscore.connector.azure_storage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import it.pagopa.selfcare.mscore.api.FileStorageConnector;
import it.pagopa.selfcare.mscore.config.AzureStorageConfig;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static it.pagopa.selfcare.mscore.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;

@Slf4j
@Service
@PropertySource("classpath:config/azure-storage-config.properties")
@Profile("AzureStorage")
public class AzureBlobClient implements FileStorageConnector {

    private final CloudBlobClient blobClient;
    private final AzureStorageConfig azureStorageConfig;

    AzureBlobClient(AzureStorageConfig azureStorageConfig) throws URISyntaxException, InvalidKeyException, StorageException {
        log.trace("AzureBlobClient.AzureBlobClient");
        this.azureStorageConfig = azureStorageConfig;
        final CloudStorageAccount storageAccount = buildStorageAccount();
        this.blobClient = storageAccount.createCloudBlobClient();
    }

    private CloudStorageAccount buildStorageAccount() throws URISyntaxException, InvalidKeyException, StorageException {
        StorageCredentials storageCredentials = StorageCredentials.tryParseCredentials(azureStorageConfig.getConnectionString());
        return new CloudStorageAccount(storageCredentials,
                true,
                azureStorageConfig.getEndpointSuffix(),
                azureStorageConfig.getAccountName());
    }

    @Override
    public String getTemplateFile(String templateName) {
        log.info("START - getTemplateFile for template: {}", templateName);
        try {
            final CloudBlobContainer blobContainer = blobClient.getContainerReference(azureStorageConfig.getContainer());
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(templateName);
            String downloaded = blob.downloadText();
            log.info("END - getTemplateFile - Downloaded {}", templateName);
            return downloaded;
        } catch (StorageException | URISyntaxException | IOException e) {
            log.error(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName), e);
            throw new MsCoreException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

}
