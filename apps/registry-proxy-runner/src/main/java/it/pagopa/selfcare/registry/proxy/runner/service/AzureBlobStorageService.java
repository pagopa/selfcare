package it.pagopa.selfcare.registry.proxy.runner.service;

import com.azure.core.util.BinaryData;
import com.azure.identity.ManagedIdentityCredentialBuilder;
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
 *
 * <p><b>Authentication</b>:
 *
 * <ul>
 *   <li>Local/dev: set {@code BLOB_STORAGE_CONN_STRING} — connection string takes precedence.
 *   <li>Azure (production): set {@code AZURE_STORAGE_ACCOUNT_NAME} + {@code AZURE_CLIENT_ID} —
 *       authenticates via the user-assigned Managed Identity.
 * </ul>
 */
@Slf4j
@ApplicationScoped
public class AzureBlobStorageService {

  private final BlobContainerClient containerClient;
  private final boolean enabled;

  public AzureBlobStorageService(
      @ConfigProperty(name = "blob-storage.connection-string", defaultValue = "")
          String connectionString,
      @ConfigProperty(name = "blob-storage.account-name", defaultValue = "")
          String accountName,
      @ConfigProperty(name = "blob-storage.container-name", defaultValue = "")
          String containerName,
      @ConfigProperty(name = "blob-storage.managed-identity-client-id", defaultValue = "")
          String managedIdentityClientId) {

    if (containerName.isBlank()) {
      this.containerClient = null;
      this.enabled = false;
      log.warn(
          "Azure Blob Storage not configured (blob-storage.container-name missing)"
              + " — open-data daily CSV snapshots will not be saved");
      return;
    }

    BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

    if (!connectionString.isBlank()) {
      // Local development: authenticate via connection string
      log.info("Azure Blob Storage: using connection string (local mode)");
      builder.connectionString(connectionString);
    } else if (!accountName.isBlank()) {
      // Azure: authenticate via user-assigned Managed Identity
      String endpoint = "https://" + accountName + ".blob.core.windows.net";
      var credentialBuilder = new ManagedIdentityCredentialBuilder();
      if (!managedIdentityClientId.isBlank()) {
        credentialBuilder.clientId(managedIdentityClientId);
      }
      log.info("Azure Blob Storage: using Managed Identity (account: {})", accountName);
      builder.endpoint(endpoint).credential(credentialBuilder.build());
    } else {
      this.containerClient = null;
      this.enabled = false;
      log.warn(
          "Azure Blob Storage not configured (neither blob-storage.connection-string nor"
              + " blob-storage.account-name is set) — open-data daily CSV snapshots will not be saved");
      return;
    }

    this.containerClient = builder.buildClient().getBlobContainerClient(containerName);
    this.enabled = true;
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
