package it.pagopa.selfcare.party.connector.azure_storage.client;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ProxyRegistryException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

@Slf4j
@Service
@PropertySource("classpath:config/azure-storage-config.properties")
@Profile("AzureStorage")
public class AzureBlobClient implements FileStorageConnector {

    private static final String ERROR_DURING_DOWNLOAD_FILE_MESSAGE = "Error during download file %s";
    private static final String ERROR_DURING_DOWNLOAD_FILE_CODE = "0000";
    private static final String ERROR_DURING_UPLOAD_FILE_MESSAGE = "Error during upload file %s";
    private static final String ERROR_DURING_UPLOAD_FILE_CODE = "0000";
    private final CloudBlobClient blobClient;
    private final String containerReference;

    public AzureBlobClient(@Value("${blobStorage.connectionString}") String storageConnectionString,
                           @Value("${blobStorage.containerReference}") String containerReference)
            throws URISyntaxException, InvalidKeyException {
        if (log.isDebugEnabled()) {
            log.trace("AzureBlobClient");
            log.debug("AzureBlobClient storageConnectionString = {}, containerReference = {}",
                    storageConnectionString, containerReference);
        }
        final CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        this.blobClient = storageAccount.createCloudBlobClient();
        this.containerReference = containerReference;
    }

    @Override
    public ResourceResponse getFile(String fileName) {
        log.info("START - getFile for path: {}", fileName);
        ResourceResponse response = new ResourceResponse();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final CloudBlobContainer blobContainer = blobClient.getContainerReference(containerReference);
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(fileName);
            BlobProperties properties = blob.getProperties();
            blob.download(outputStream);
            log.info("END - getFile - path {}", fileName);
            response.setData(outputStream.toByteArray());
            response.setFileName(blob.getName());
            response.setMimetype(properties.getContentType());
            return response;
        } catch (StorageException e) {
            if (e.getHttpStatusCode() == 404) {
                throw new ResourceNotFoundException(String.format(ERROR_DURING_DOWNLOAD_FILE_MESSAGE, fileName));
            }
            throw new ProxyRegistryException(String.format(ERROR_DURING_DOWNLOAD_FILE_MESSAGE, fileName),
                    ERROR_DURING_DOWNLOAD_FILE_CODE);
        } catch (URISyntaxException | IOException e) {
            throw new ProxyRegistryException(String.format(ERROR_DURING_DOWNLOAD_FILE_MESSAGE, fileName),
                    ERROR_DURING_DOWNLOAD_FILE_CODE);
        }
    }

    @Override
    public void uploadFile(InputStream file, String fileName) {
        log.info("START - uploadFile for path: {}", fileName);
        try {
            final CloudBlobContainer blobContainer = blobClient.getContainerReference(containerReference);
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(fileName);
            blob.upload(file, file.available());
            log.info("Uploaded {}", fileName);
        } catch (StorageException | URISyntaxException | IOException e) {
            log.error(String.format(ERROR_DURING_UPLOAD_FILE_MESSAGE, fileName), e);
            throw new ProxyRegistryException(String.format(ERROR_DURING_UPLOAD_FILE_MESSAGE, fileName),
                    ERROR_DURING_UPLOAD_FILE_CODE);
        }
    }
}
