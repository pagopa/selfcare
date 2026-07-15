package it.pagopa.selfcare.dashboard.client.azure_storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import it.pagopa.selfcare.dashboard.connector.api.FileStorageConnector;
import it.pagopa.selfcare.dashboard.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Service
@Profile("AzureStorage")
class AzureBlobClient implements FileStorageConnector {

    private final String institutionsLogoContainerReference;
    private final BlobServiceClient blobClient;


    AzureBlobClient(@Value("${blobStorage.connectionString}") String storageConnectionString,
                    @Value("${blobStorage.institutions.logo.containerReference}") String institutionsLogoContainerReference,
                    @Value("${blobStorage.accountNameProduct}") String accountNameProduct,
                    @Value("${blobStorage.managedIdentityClientIdProduct}") String managedIdentityClientIdProduct) {
        if (log.isDebugEnabled()) {
            log.trace("AzureBlobClient");
            log.debug("AzureBlobClient storageConnectionString = {}, containerReference = {}, accountNameProduct = {}, managedIdentityClientIdProduct = {}",
                    storageConnectionString, institutionsLogoContainerReference, accountNameProduct, managedIdentityClientIdProduct);
        }
        this.blobClient = Optional.ofNullable(storageConnectionString)
            .filter(cs -> !cs.isBlank())
            .map(cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient())
            .orElseGet(() -> new BlobServiceClientBuilder()
                .endpoint("https://" + accountNameProduct + ".blob.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(managedIdentityClientIdProduct)
                    .build()
                ).buildClient());
        this.institutionsLogoContainerReference = institutionsLogoContainerReference;
    }

    @Override
    public void uploadInstitutionLogo(InputStream file, String fileName, String contentType) throws FileUploadException {
        if (log.isDebugEnabled()) {
            log.trace("uploadInstitutionLogo");
            log.debug("uploadInstitutionLogo fileName = {}, contentType = {}", Encode.forJava(fileName), Encode.forJava(contentType));
        }

        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(institutionsLogoContainerReference);
            final BlobClient blob = blobContainer.getBlobClient(fileName);
            blob.upload(file);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
            log.info("Uploaded {}", Encode.forJava(fileName));

        } catch (Exception ex) {
            throw new FileUploadException(ex);
        }
    }

}
