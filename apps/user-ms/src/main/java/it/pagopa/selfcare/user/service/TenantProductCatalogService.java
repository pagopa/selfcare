package it.pagopa.selfcare.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceDefault;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
public class TenantProductCatalogService implements ProductService {
  @Inject TenantRegistry registry;
  @Inject TenantContext context;
  @ConfigProperty(name = "user-ms.blob-storage.filepath-product") String filePath;
  @ConfigProperty(name = "user-ms.products.preload-enabled", defaultValue = "true") boolean preloadEnabled;
  private final Map<String, AzureBlobClientDefault> clients = new ConcurrentHashMap<>();
  private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
  private final Map<String, AtomicBoolean> refreshing = new ConcurrentHashMap<>();

  void initialize(@Observes StartupEvent event) {
    if (preloadEnabled) {
      registry.supportedTenantIds().forEach(this::refreshTenant);
    }
  }

  @Scheduled(every = "${user-ms.products.refresh-interval:1m}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
  public void refreshCatalogs() {
    for (String tenant : registry.supportedTenantIds()) {
      try {
        refreshTenant(tenant);
      } catch (RuntimeException failure) {
        log.error("Failed to refresh product catalogue for tenant {}; retaining the last loaded snapshot", tenant, failure);
      }
    }
  }

  public void refreshTenant(String tenant) {
    registry.resolve(tenant);
    String id = registry.normalizeTenantId(tenant);
    AtomicBoolean inProgress = refreshing.computeIfAbsent(id, ignored -> new AtomicBoolean());
    if (!inProgress.compareAndSet(false, true)) {
      return;
    }
    try {
      var client = blobClient(id);
      String path = productPath(id);
      Instant modified = client.getProperties(path).getLastModified().toInstant();
      Snapshot previous = snapshots.get(id);
      if (previous != null && previous.modified().equals(modified)) {
        return;
      }
      ProductServiceDefault service = new ProductServiceDefault(client.getFileAsText(path));
      // Resolve parent links before publication, rather than building them on request threads.
      service.getProducts(false, false).forEach(product -> service.getProduct(product.getId()));
      snapshots.put(id, new Snapshot(modified, service));
    } catch (JsonProcessingException failure) {
      throw new IllegalStateException("Invalid product catalogue for tenant " + id, failure);
    } finally {
      inProgress.set(false);
    }
  }

  public boolean hasSnapshot(String tenant) {
    registry.resolve(tenant);
    return snapshots.containsKey(registry.normalizeTenantId(tenant));
  }

  public AzureBlobClientDefault blobClient(String tenant) {
    registry.resolve(tenant);
    return clients.computeIfAbsent(registry.normalizeTenantId(tenant), this::createClient);
  }

  protected AzureBlobClientDefault createClient(String tenant) {
    var storage = registry.storage(tenant, "products");
    return switch (storage.authentication().type()) {
      case CONNECTION_STRING -> new AzureBlobClientDefault(
          registry.storageConnectionString(tenant, "products").orElseThrow(), storage.container());
      case MANAGED_IDENTITY -> new AzureBlobClientDefault(storage.container(), storage.account(),
          registry.storageManagedIdentityClientId(tenant, "products").orElse(""));
    };
  }

  public String productPath(String tenant) {
    String prefix = registry.storage(tenant, "products").pathPrefix();
    if (prefix == null || prefix.isBlank()) {
      return filePath;
    }
    return prefix.replaceAll("/+$", "") + "/" + filePath;
  }

  public List<Product> productsForTenant(String tenant) {
    return invoke(tenant, service -> service.getProducts(false, true));
  }

  private <T> T invoke(Function<ProductServiceDefault, T> operation) {
    return invoke(context.requiredTenantId(), operation);
  }

  private <T> T invoke(String tenant, Function<ProductServiceDefault, T> operation) {
    registry.resolve(tenant);
    String id = registry.normalizeTenantId(tenant);
    Snapshot snapshot = snapshots.get(id);
    if (snapshot == null) {
      log.error("Product catalogue has not been loaded for tenant {}", id);
      throw new IllegalStateException("Product catalogue has not been loaded for tenant " + id);
    }
    return operation.apply(snapshot.service());
  }

  private record Snapshot(Instant modified, ProductServiceDefault service) {}

  @Override public List<Product> getProducts(boolean rootOnly, boolean valid) {
    return invoke(service -> service.getProducts(rootOnly, valid));
  }
  @Override public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> mappings) {
    invoke(service -> { service.validateRoleMappings(mappings); return null; });
  }
  @Override public Product getProduct(String id) { return invoke(service -> service.getProduct(id)); }
  @Override public Product getProductRaw(String id) { return invoke(service -> service.getProductRaw(id)); }
  @Override public Product getProductIsValid(String id) { return invoke(service -> service.getProductIsValid(id)); }
  @Override public ProductRole validateProductRole(String id, String role, PartyRole partyRole) {
    return invoke(service -> service.validateProductRole(id, role, partyRole));
  }
  @Override public boolean verifyAllowedByInstitutionTaxCode(String id, String taxCode) {
    return invoke(service -> service.verifyAllowedByInstitutionTaxCode(id, taxCode));
  }
  @Override public Integer getProductExpirationDate(String id) {
    return invoke(service -> service.getProductExpirationDate(id));
  }
  @Override public boolean isProductEnabled(String id) { return invoke(service -> service.isProductEnabled(id)); }
}
