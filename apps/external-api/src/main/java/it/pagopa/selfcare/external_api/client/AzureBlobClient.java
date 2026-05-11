package it.pagopa.selfcare.external_api.client;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import it.pagopa.selfcare.external_api.connector.FileStorageConnector;
import it.pagopa.selfcare.external_api.exception.AzureRestClientException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.model.document.ResourceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static it.pagopa.selfcare.external_api.model.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;

@Slf4j
@Service
@Profile("AzureStorage")
public class AzureBlobClient implements FileStorageConnector {

    private final CloudBlobClient blobClient;
    private final String institutionContractContainerReference;

    AzureBlobClient(@Value("${blobStorage.connectionString}") String storageConnectionString,
                    @Value("${blobStorage.institutions.contract.containerReference}") String institutionContractContainerReference)
            throws URISyntaxException, InvalidKeyException {
        final CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        this.blobClient = storageAccount.createCloudBlobClient();
        this.institutionContractContainerReference = institutionContractContainerReference;
    }

    @Override
    public ResourceResponse getFile(String fileName) {
        log.info("START - getFile for path: {}", fileName);
        try {
            ResourceResponse response = new ResourceResponse();
            final CloudBlobContainer blobContainer = blobClient.getContainerReference(institutionContractContainerReference);
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BlobProperties properties = blob.getProperties();
            blob.download(outputStream);
            log.info("END - getFile - path {}", fileName);
            response.setData(outputStream.toByteArray());
            response.setFileName(blob.getName());
            response.setMimetype(properties.getContentType());
            return response;
        } catch (StorageException e) {
            if (e.getHttpStatusCode() == 404) {
                throw new ResourceNotFoundException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                        ERROR_DURING_DOWNLOAD_FILE.getCode());
            }
            throw new AzureRestClientException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        } catch (URISyntaxException e) {
            throw new AzureRestClientException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }
}
