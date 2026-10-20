package it.pagopa.selfcare.mscore.core.config;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClient;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TenantProductCatalogService implements ProductService {

    private static final String PRODUCTS_STORAGE_KEY = "products";

    private final TenantBlobClientProvider blobClientProvider;
    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private final String productFilePath;
    private final ConcurrentHashMap<String, ProductServiceCacheable> services =
            new ConcurrentHashMap<>();

    public TenantProductCatalogService(
            TenantBlobClientProvider blobClientProvider,
            TenantRegistry tenantRegistry,
            TenantContext tenantContext,
            String productFilePath) {
        this.blobClientProvider = blobClientProvider;
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
        this.productFilePath = productFilePath;
    }

    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {
        return invoke(service -> service.getProducts(rootOnly, valid));
    }

    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {
        invoke(service -> {
            service.validateRoleMappings(roleMappings);
            return null;
        });
    }

    @Override
    public Product getProduct(String productId) {
        return invoke(service -> service.getProduct(productId));
    }

    @Override
    public Product getProductRaw(String productId) {
        return invoke(service -> service.getProductRaw(productId));
    }

    @Override
    public Product getProductIsValid(String productId) {
        return invoke(service -> service.getProductIsValid(productId));
    }

    @Override
    public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
        return invoke(service -> service.validateProductRole(productId, productRole, role));
    }

    @Override
    public boolean verifyAllowedByInstitutionTaxCode(String productId, String taxCode) {
        return invoke(service -> service.verifyAllowedByInstitutionTaxCode(productId, taxCode));
    }

    @Override
    public Integer getProductExpirationDate(String productId) {
        return invoke(service -> service.getProductExpirationDate(productId));
    }

    @Override
    public boolean isProductEnabled(String productId) {
        return invoke(service -> service.isProductEnabled(productId));
    }

    private <T> T invoke(Function<ProductServiceCacheable, T> operation) {
        String tenantId =
                tenantRegistry.normalizeAndValidate(tenantContext.requiredTenantId());
        ProductServiceCacheable service = services.computeIfAbsent(
                tenantId,
                id -> new ProductServiceCacheable(
                        new ProductCatalogBlobClient(
                                blobClientProvider.clientFor(id, PRODUCTS_STORAGE_KEY)),
                        productFilePath));
        synchronized (service) {
            return operation.apply(service);
        }
    }

    private static final class ProductCatalogBlobClient implements AzureBlobClient {

        private final TenantBlobClient blobClient;

        private ProductCatalogBlobClient(TenantBlobClient blobClient) {
            this.blobClient = blobClient;
        }

        @Override
        public String getFileAsText(String filePath) {
            return blobClient.blob(filePath).downloadContent().toString();
        }

        @Override
        public BlobProperties getProperties(String filePath) {
            return blobClient.blob(filePath).getProperties();
        }

        @Override
        public byte[] getFile(String filePath) {
            return blobClient.blob(filePath).downloadContent().toBytes();
        }

        @Override
        public List<String> getFiles() {
            return blobClient.list().map(item -> item.getName()).toList();
        }

        @Override
        public List<String> getFiles(String path) {
            return blobClient.list()
                    .map(item -> item.getName())
                    .filter(name -> name.startsWith(path))
                    .toList();
        }

        @Override
        public java.io.File retrieveFile(String filePath) {
            return downloadToTempFile(filePath);
        }

        @Override
        public java.io.File getFileAsPdf(String filePath) {
            return downloadToTempFile(filePath);
        }

        private java.io.File downloadToTempFile(String filePath) {
            try {
                java.io.File file = java.io.File.createTempFile("tenant-product-", ".tmp");
                blobClient.blob(filePath).downloadToFile(file.getAbsolutePath(), true);
                file.deleteOnExit();
                return file;
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Unable to download tenant product blob", exception);
            }
        }

        @Override
        public String uploadFile(String path, String filename, byte[] data) {
            return uploadFilePath(java.nio.file.Paths.get(path, filename).toString(), data);
        }

        @Override
        public String uploadFilePath(String filePath, byte[] data) {
            BlobClient blob = blobClient.blob(filePath);
            blob.upload(com.azure.core.util.BinaryData.fromBytes(data), true);
            return filePath;
        }

        @Override
        public void removeFile(String fileName) {
            blobClient.blob(fileName).deleteIfExists();
        }
    }
}
