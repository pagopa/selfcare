package it.pagopa.selfcare.user.service;

import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantProductCatalogService implements ProductService {
  @Inject TenantRegistry registry;
  @Inject TenantContext context;
  @ConfigProperty(name = "user-ms.blob-storage.filepath-product") String filePath;
  private final Map<String, AzureBlobClientDefault> clients = new ConcurrentHashMap<>();
  private final Map<String, ProductServiceCacheable> services = new ConcurrentHashMap<>();

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

  private <T> T invoke(Function<ProductServiceCacheable, T> operation) {
    return invoke(context.requiredTenantId(), operation);
  }

  private <T> T invoke(String tenant, Function<ProductServiceCacheable, T> operation) {
    registry.resolve(tenant);
    ProductServiceCacheable service = services.computeIfAbsent(registry.normalizeTenantId(tenant),
        id -> new ProductServiceCacheable(blobClient(id), productPath(id)));
    synchronized (service) {
      return operation.apply(service);
    }
  }

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
