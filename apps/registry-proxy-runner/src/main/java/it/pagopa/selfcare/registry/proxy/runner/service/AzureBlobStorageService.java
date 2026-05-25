package it.pagopa.selfcare.registry.proxy.runner.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Uploads daily open-data CSV snapshots to Azure Blob Storage.
 *
 * <p>Blob name pattern: {@code <blobPrefix>/<yyyy-MM-dd>.csv}
 *
 * <p>If the same job runs multiple times on the same day, the existing blob is overwritten so that
 * exactly one file per day is retained.
 */
@Slf4j
@ApplicationScoped
public class AzureBlobStorageService {

  private final BlobContainerClient containerClient;
  private final boolean enabled;

  public AzureBlobStorageService(
      @ConfigProperty(name = "blob-storage.connection-string", defaultValue = "")
          String connectionString,
      @ConfigProperty(name = "blob-storage.container-name", defaultValue = "")
          String containerName) {
    if (!connectionString.isBlank() && !containerName.isBlank()) {
      this.containerClient =
          new BlobServiceClientBuilder()
              .connectionString(connectionString)
              .buildClient()
              .getBlobContainerClient(containerName);
      this.enabled = true;
    } else {
      this.containerClient = null;
      this.enabled = false;
      log.warn(
          "Azure Blob Storage not configured (blob-storage.connection-string / blob-storage.container-name missing)"
              + " — open-data daily CSV snapshots will not be saved");
    }
  }

  /**
   * Saves {@code data} as {@code <blobPrefix>/<today>.csv}, overwriting any existing blob for
   * today (idempotent across multiple same-day executions).
   *
   * <p>Failures are logged but never propagated so that a storage error never aborts the index
   * update pipeline.
   */
  public void saveDaily(byte[] data, String blobPrefix) {
    if (!enabled || data == null || data.length == 0) {
      return;
    }
    String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    String blobName = blobPrefix + "/" + date + ".csv";
    try {
      containerClient.getBlobClient(blobName).upload(BinaryData.fromBytes(data), true);
      log.info("Saved daily open-data snapshot to blob: {}", blobName);
    } catch (Exception e) {
      log.error("Failed to save daily open-data snapshot to blob: {}", blobName, e);
    }
  }
}
