package it.pagopa.selfcare.mscore.core.config;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.product.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsCoreConfig {

    @Bean
    public ProductService productService(
            TenantBlobClientProvider blobClientProvider,
            TenantRegistry tenantRegistry,
            TenantContext tenantContext,
            @Value("${mscore.blob-storage.filepath-product}") String productFilePath) {
        return new TenantProductCatalogService(
                blobClientProvider, tenantRegistry, tenantContext, productFilePath);
    }
}
