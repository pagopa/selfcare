package it.pagopa.selfcare.product.storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
public class TenantBlobClientProvider {

  private final TenantRegistry tenantRegistry;
  private final TenantContext tenantContext;
  private final ConcurrentHashMap<ClientKey, BlobServiceAsyncClient> clients =
      new ConcurrentHashMap<>();

  @ConfigProperty(name = "tenant.storage.eager-init", defaultValue = "true")
  boolean eagerInit = true;

  @Inject
  public TenantBlobClientProvider(TenantRegistry tenantRegistry, TenantContext tenantContext) {
    this.tenantRegistry = tenantRegistry;
    this.tenantContext = tenantContext;
  }

  @PostConstruct
  void initialize() {
    if (!eagerInit) {
      return;
    }
    tenantRegistry
        .supportedTenantIds()
        .forEach(
            tenantId ->
                tenantRegistry
                    .mandatoryStorageKeys()
                    .forEach(logicalKey -> clientFor(tenantId, logicalKey)));
  }

  public TenantBlobBinding clientForCurrentTenant(String logicalStorageKey) {
    return clientFor(tenantContext.requiredTenantId(), logicalStorageKey);
  }

  public TenantBlobBinding clientFor(String tenantId, String logicalStorageKey) {
    TenantDefinition.StorageDefinition storage =
        tenantRegistry.storage(tenantId, logicalStorageKey);
    BlobServiceAsyncClient client =
        clients.computeIfAbsent(
            clientKey(storage), key -> createClient(tenantId, logicalStorageKey, storage));
    return new TenantBlobBinding(
        client, storage.container(), normalizePrefix(storage.pathPrefix()));
  }

  protected BlobServiceAsyncClient createClient(
      String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
    TenantDefinition.StorageAuthentication authentication = storage.authentication();
    return switch (authentication.type()) {
      case CONNECTION_STRING -> {
        String connectionString =
            tenantRegistry
                .storageConnectionString(tenantId, logicalStorageKey)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Missing storage connection string for tenant "
                                + tenantId
                                + " and key "
                                + logicalStorageKey));
        log.info(
            "Creating Azure Blob client with connection string: tenant={}, key={}, container={}",
            tenantId,
            logicalStorageKey,
            storage.container());
        yield new BlobServiceClientBuilder().connectionString(connectionString).buildAsyncClient();
      }
      case MANAGED_IDENTITY -> {
        String managedIdentityClientId =
            tenantRegistry.storageManagedIdentityClientId(tenantId, logicalStorageKey).orElse("");
        log.info(
            "Creating Azure Blob client with managed identity: tenant={}, key={}, account={}, container={}, managedIdentityClientIdConfigured={}",
            tenantId,
            logicalStorageKey,
            storage.account(),
            storage.container(),
            !managedIdentityClientId.isBlank());
        DefaultAzureCredentialBuilder credentialBuilder = new DefaultAzureCredentialBuilder();
        if (!managedIdentityClientId.isBlank()) {
          credentialBuilder.managedIdentityClientId(managedIdentityClientId);
        }
        yield new BlobServiceClientBuilder()
            .endpoint("https://" + storage.account() + ".blob.core.windows.net")
            .credential(credentialBuilder.build())
            .buildAsyncClient();
      }
    };
  }

  private static String normalizePrefix(String pathPrefix) {
    if (pathPrefix == null || pathPrefix.isBlank()) {
      return "";
    }
    if (pathPrefix.contains("..")) {
      throw new IllegalArgumentException("Invalid storage path prefix");
    }
    return pathPrefix.endsWith("/") ? pathPrefix.substring(0, pathPrefix.length() - 1) : pathPrefix;
  }

  private static ClientKey clientKey(TenantDefinition.StorageDefinition storage) {
    TenantDefinition.StorageAuthentication authentication = storage.authentication();
    String credentialRef =
        switch (authentication.type()) {
          case CONNECTION_STRING -> authentication.connectionStringEnvVar();
          case MANAGED_IDENTITY ->
              Objects.toString(authentication.managedIdentityClientIdEnvVar(), "");
        };
    return new ClientKey(
        storage.account(), storage.container(), authentication.type().name(), credentialRef);
  }

  private record ClientKey(
      String account, String container, String authenticationType, String credentialRef) {}
}
