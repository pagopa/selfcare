package it.pagopa.selfcare.product;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantProductCdcResourceProvider {
  private static final String PRODUCTS_STORAGE_KEY = "products";

  private final TenantRegistry tenantRegistry;
  private final TenantMongoClientProducer mongoClientProducer;
  private final String productsFilePath;
  private final ConcurrentHashMap<String, TenantProductCdcResources> resources =
      new ConcurrentHashMap<>();

  public TenantProductCdcResourceProvider(
      TenantRegistry tenantRegistry,
      TenantMongoClientProducer mongoClientProducer,
      @ConfigProperty(name = "product-cdc.blob-storage.filepath-product") String productsFilePath) {
    this.tenantRegistry = tenantRegistry;
    this.mongoClientProducer = mongoClientProducer;
    this.productsFilePath = productsFilePath;
  }

  public TenantProductCdcResources forTenant(String tenantId) {
    String normalizedTenantId = tenantRegistry.normalizeTenantId(tenantId);
    return resources.computeIfAbsent(normalizedTenantId, this::createResources);
  }

  private TenantProductCdcResources createResources(String tenantId) {
    TenantDefinition.StorageDefinition storage =
        tenantRegistry.storage(tenantId, PRODUCTS_STORAGE_KEY);
    AzureBlobClient blobClient = createBlobClient(tenantId, storage);
    String path = prefixedPath(storage.pathPrefix(), productsFilePath);
    return new TenantProductCdcResources(
        tenantId,
        mongoClientProducer.clientForTenant(tenantId),
        tenantRegistry.resolve(tenantId).mongo().database(),
        new ProductServiceCacheable(blobClient, path),
        blobClient,
        path);
  }

  private AzureBlobClient createBlobClient(
      String tenantId, TenantDefinition.StorageDefinition storage) {
    TenantDefinition.StorageAuthentication authentication = storage.authentication();
    return switch (authentication.type()) {
      case CONNECTION_STRING ->
          new AzureBlobClientDefault(
              tenantRegistry
                  .storageConnectionString(tenantId, PRODUCTS_STORAGE_KEY)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Missing product storage connection string for " + tenantId)),
              storage.container());
      case MANAGED_IDENTITY ->
          new AzureBlobClientDefault(
              storage.container(),
              storage.account(),
              tenantRegistry
                  .storageManagedIdentityClientId(tenantId, PRODUCTS_STORAGE_KEY)
                  .orElse(""));
    };
  }

  private static String prefixedPath(String prefix, String filePath) {
    if (prefix == null || prefix.isBlank()) {
      return filePath;
    }
    if (prefix.contains("..") || Path.of(filePath).isAbsolute()) {
      throw new IllegalArgumentException("Invalid product storage path");
    }
    return prefix.replaceAll("/+$", "") + "/" + filePath.replaceAll("^/+", "");
  }
}
