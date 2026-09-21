package it.pagopa.selfcare.onboarding.storage;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantAwareProductService implements ProductService {

    private final TenantContext tenantContext;
    private final TenantBlobClientProvider blobClientProvider;
    private final String filepathProduct;
    private final ConcurrentHashMap<String, ProductServiceCacheable> productServices = new ConcurrentHashMap<>();

    @Inject
    public TenantAwareProductService(
            TenantContext tenantContext,
            TenantBlobClientProvider blobClientProvider,
            @ConfigProperty(name = "onboarding-ms.blob-storage.filepath-product") String filepathProduct) {
        this.tenantContext = tenantContext;
        this.blobClientProvider = blobClientProvider;
        this.filepathProduct = filepathProduct;
    }

    private ProductService delegate() {
        String tenantId = tenantContext.requiredTenantId();
        return productServices.computeIfAbsent(
                tenantId,
                id -> new ProductServiceCacheable(
                        blobClientProvider.clientFor(id, StorageKeys.PRODUCTS), filepathProduct));
    }

    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {
        return delegate().getProducts(rootOnly, valid);
    }

    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {
        delegate().validateRoleMappings(roleMappings);
    }

    @Override
    public Product getProduct(String productId) {
        return delegate().getProduct(productId);
    }

    @Override
    public Product getProductRaw(String productId) {
        return delegate().getProductRaw(productId);
    }

    @Override
    public Product getProductIsValid(String productId) {
        return delegate().getProductIsValid(productId);
    }

    @Override
    public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
        return delegate().validateProductRole(productId, productRole, role);
    }

    @Override
    public boolean verifyAllowedByInstitutionTaxCode(String productId, String taxCode) {
        return delegate().verifyAllowedByInstitutionTaxCode(productId, taxCode);
    }

    @Override
    public Integer getProductExpirationDate(String productId) {
        return delegate().getProductExpirationDate(productId);
    }

    @Override
    public boolean isProductEnabled(String productId) {
        return delegate().isProductEnabled(productId);
    }
}
