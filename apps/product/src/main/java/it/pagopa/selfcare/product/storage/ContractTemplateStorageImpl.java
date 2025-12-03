package it.pagopa.selfcare.product.storage;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.exception.ConflictException;
import it.pagopa.selfcare.product.exception.InternalException;
import it.pagopa.selfcare.product.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;

@ApplicationScoped
@Slf4j
public class ContractTemplateStorageImpl implements ContractTemplateStorage {

    private static final String DST_FILE_PATH = "contract-templates/%s/%s";

    private final String containerName;
    private final BlobServiceAsyncClient blobClient;

    public ContractTemplateStorageImpl(@ConfigProperty(name = "product-ms.blob-storage.container-contract-template") String containerName,
                                       @ConfigProperty(name = "product-ms.blob-storage.connection-string-contract-template") String connectionString) {
        this.containerName = containerName;
        this.blobClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildAsyncClient();
    }

    @Override
    public Uni<Void> upload(String productId, String contractTemplateId, ContractTemplateFile contractTemplateFile) {
        final BlobContainerAsyncClient blobContainer = blobClient.getBlobContainerAsyncClient(containerName);
        final String filePath = String.format(DST_FILE_PATH, productId, contractTemplateId);
        final BlobAsyncClient blob = blobContainer.getBlobAsyncClient(filePath);
        final BlobHttpHeaders headers = new BlobHttpHeaders();
        headers.setContentType(contractTemplateFile.getContentType());
        return Uni.createFrom()
                .completionStage(contractTemplateFile.getFile() != null ?
                        blob.uploadFromFile(contractTemplateFile.getFile().getAbsolutePath(), null, headers, null, null, null).toFuture()
                        :
                        blob.uploadWithResponse(Flux.just(ByteBuffer.wrap(contractTemplateFile.getData())), null, headers, null, null, null).toFuture()
                )
                .onItem().invoke(v -> log.info("Contract template uploaded to blob storage at path: {}", filePath))
                .onFailure(BlobStorageException.class).transform(t -> {
                    if (((BlobStorageException) t).getStatusCode() == 409) {
                        log.error("Contract template already exists in blob storage at path {}", filePath);
                        return new ConflictException("Contract template already exists on storage", "409");
                    }
                    log.error("Error uploading contract template to blob storage at path {}", filePath, t);
                    return new InternalException("Error uploading contract template", "500");
                })
                .replaceWithVoid();
    }

    @Override
    public Uni<ContractTemplateFile> download(String productId, String contractTemplateId) {
        final BlobContainerAsyncClient blobContainer = blobClient.getBlobContainerAsyncClient(containerName);
        final String filePath = String.format(DST_FILE_PATH, productId, contractTemplateId);
        final BlobAsyncClient blob = blobContainer.getBlobAsyncClient(filePath);
        final DownloadRetryOptions retryOptions = new DownloadRetryOptions();
        retryOptions.setMaxRetryRequests(3);
        return Uni.createFrom()
                .completionStage(blob.downloadContentWithResponse(retryOptions, null).toFuture())
                .onItem().transform(r -> {
                    byte[] data = r.getValue().toBytes();
                    String contentType = r.getDeserializedHeaders().getContentType();
                    return ContractTemplateFile.builder()
                            .data(data)
                            .contentType(contentType)
                            .build();
                })
                .onItem().invoke(bd -> log.info("Contract template downloaded from blob storage: {}", filePath))
                .onFailure(BlobStorageException.class).transform(t -> {
                    if (((BlobStorageException) t).getStatusCode() == 404) {
                        log.warn("Contract template not found in blob storage at path {}", filePath);
                        return new ResourceNotFoundException("Contract template not found", "404");
                    }
                    log.error("Error downloading contract template from blob storage at path {}", filePath, t);
                    return new InternalException("Error downloading contract template", "500");
                });
    }

}
