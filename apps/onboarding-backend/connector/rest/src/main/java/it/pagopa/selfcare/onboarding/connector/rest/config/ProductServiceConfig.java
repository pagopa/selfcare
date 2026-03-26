package it.pagopa.selfcare.onboarding.connector.rest.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/products-sdk.properties")
public class ProductServiceConfig {
    @Value("${onboarding-backend.blob-storage.container-product}")
    private String containerProduct;
    @Value("${onboarding-backend.blob-storage.filepath-product}")
    private String filepathProduct;
    @Value("${onboarding-backend.blob-storage.connection-string-product}")
    private String connectionStringProduct;

    @Bean
    public ProductService productService() {
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(connectionStringProduct, containerProduct);
        try{
            return new ProductServiceCacheable(azureBlobClient, filepathProduct);
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
}