package it.pagopa.selfcare.mscore.core.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class MsCoreConfig {

    private final CoreConfig config;

    @Bean
    public ProductService productService(AzureBlobClientDefault productBlobClient) {
        return new ProductServiceCacheable(
                productBlobClient, config.getBlobStorage().getFilepathProduct());
    }

    @Bean
    public AzureBlobClientDefault productBlobClient() {
        return Optional.ofNullable(config.getBlobStorage().getConnectionStringProduct())
            .filter(cs -> !cs.isBlank())
            .map(cs -> new AzureBlobClientDefault(
                    cs, config.getBlobStorage().getContainerProduct()))
            .orElseGet(() -> new AzureBlobClientDefault(
                    config.getBlobStorage().getContainerProduct(),
                    config.getBlobStorage().getAccountNameProduct(),
                    config.getBlobStorage().getManagedIdentityClientIdProduct()));
    }

}
