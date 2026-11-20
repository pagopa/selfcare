package it.pagopa.selfcare.external_api.client;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.external_api.connector.FileStorageConnector;
import it.pagopa.selfcare.external_api.exception.AzureRestClientException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.model.document.ResourceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

import static it.pagopa.selfcare.external_api.model.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;

@Slf4j
@Service
@Profile("AzureStorage")
public class AzureBlobClient implements FileStorageConnector {

    private static final String CONTRACTS_STORAGE = "contracts";
    private final TenantBlobClientProvider blobClients;

    AzureBlobClient(TenantBlobClientProvider blobClients) {
        this.blobClients = blobClients;
    }

    @Override
    public ResourceResponse getFile(String fileName) {
        log.info("START - getFile for path: {}", fileName);
        try {
            ResourceResponse response = new ResourceResponse();
            BlobClient blob = blobClients.clientForCurrentTenant(CONTRACTS_STORAGE).blob(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            var properties = blob.getProperties();
            blob.downloadStream(outputStream);
            log.info("END - getFile - path {}", fileName);
            response.setData(outputStream.toByteArray());
            response.setFileName(blob.getBlobName());
            response.setMimetype(properties.getContentType());
            return response;
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                throw new ResourceNotFoundException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                        ERROR_DURING_DOWNLOAD_FILE.getCode());
            }
            throw new AzureRestClientException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }
}
