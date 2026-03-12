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
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.core.publisher.Flux;

@ApplicationScoped
@Slf4j
public class ContractTemplateStorageImpl implements ContractTemplateStorage {

  // file path pattern:
  // contract-templates/{productId}/{contractTemplateId}.{contractTemplateExtension}
  private static final String DST_FILE_PATH = "contract-templates/%s/%s.%s";

  private final String containerName;
  private final BlobServiceAsyncClient blobClient;

  @Inject
  public ContractTemplateStorageImpl(
      @ConfigProperty(name = "product-ms.blob-storage.container-contract-template")
          String containerName,
      @ConfigProperty(name = "product-ms.blob-storage.connection-string-contract-template")
          String connectionString) {
    this.containerName = containerName;
    this.blobClient =
        new BlobServiceClientBuilder().connectionString(connectionString).buildAsyncClient();
  }

  public ContractTemplateStorageImpl(String containerName, BlobServiceAsyncClient blobClient) {
    this.containerName = containerName;
    this.blobClient = blobClient;
  }

  @Override
  public Uni<Void> upload(
      String productId, String contractTemplateId, ContractTemplateFile contractTemplateFile) {
    final BlobContainerAsyncClient blobContainer =
        blobClient.getBlobContainerAsyncClient(containerName);
    final String filePath =
        getContractTemplatePath(
            productId, contractTemplateId, contractTemplateFile.getType().getExtension());
    final BlobAsyncClient blob = blobContainer.getBlobAsyncClient(filePath);
    final BlobHttpHeaders headers = new BlobHttpHeaders();
    headers.setContentType(contractTemplateFile.getType().getContentType());
    return Uni.createFrom()
        .completionStage(
            contractTemplateFile.getFile() != null
                ? blob.uploadFromFile(
                        contractTemplateFile.getFile().getAbsolutePath(),
                        null,
                        headers,
                        null,
                        null,
                        null)
                    .toFuture()
                : blob.uploadWithResponse(
                        Flux.just(ByteBuffer.wrap(contractTemplateFile.getData())),
                        null,
                        headers,
                        null,
                        null,
                        null)
                    .toFuture())
        .onItem()
        .invoke(v -> log.info("Contract template uploaded to blob storage at path: {}", filePath))
        .onFailure(BlobStorageException.class)
        .transform(
            t -> {
              if (((BlobStorageException) t).getStatusCode() == 409) {
                log.error("Contract template already exists in blob storage at path {}", filePath);
                return new ConflictException("Contract template already exists on storage", "409");
              }
              log.error(
                  "Error uploading contract template to blob storage at path {}", filePath, t);
              return new InternalException("Error uploading contract template", "500");
            })
        .replaceWithVoid();
  }

  @Override
  public Uni<ContractTemplateFile> download(
      String productId, String contractTemplateId, ContractTemplateFileType fileType) {
    final BlobContainerAsyncClient blobContainer =
        blobClient.getBlobContainerAsyncClient(containerName);
    final String filePath =
        getContractTemplatePath(productId, contractTemplateId, fileType.getExtension());
    final BlobAsyncClient blob = blobContainer.getBlobAsyncClient(filePath);
    final DownloadRetryOptions retryOptions = new DownloadRetryOptions();
    retryOptions.setMaxRetryRequests(3);
    return Uni.createFrom()
        .completionStage(blob.downloadContentWithResponse(retryOptions, null).toFuture())
        .onItem()
        .transform(
            r -> {
              byte[] data = r.getValue().toBytes();
              // In the future, when the file type is no longer required in the request, it can be
              // read from the header.
              // String contentType = r.getDeserializedHeaders().getContentType();
              return ContractTemplateFile.builder().data(data).type(fileType).build();
            })
        .onItem()
        .invoke(bd -> log.info("Contract template downloaded from blob storage: {}", filePath))
        .onFailure(BlobStorageException.class)
        .transform(
            t -> {
              if (((BlobStorageException) t).getStatusCode() == 404) {
                log.warn("Contract template not found in blob storage at path {}", filePath);
                return new ResourceNotFoundException("Contract template not found", "404");
              }
              log.error(
                  "Error downloading contract template from blob storage at path {}", filePath, t);
              return new InternalException("Error downloading contract template", "500");
            });
  }

  @Override
  public String getContractTemplatePath(
      String productId, String contractTemplateId, String contractTemplateExtension) {
    return String.format(DST_FILE_PATH, productId, contractTemplateId, contractTemplateExtension);
  }
}
