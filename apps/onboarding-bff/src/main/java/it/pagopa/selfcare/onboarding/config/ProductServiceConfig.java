package it.pagopa.selfcare.onboarding.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ProductServiceConfig {
    @ConfigProperty(name = "onboarding-bff.blob-storage.container-product")
    String containerProduct;
    @ConfigProperty(name = "onboarding-bff.blob-storage.filepath-product")
    String filepathProduct;
    @ConfigProperty(name = "onboarding-bff.blob-storage.connection-string-product")
    String connectionStringProduct;

    @Produces
    public ProductService productService() {
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(connectionStringProduct, containerProduct);
        try{
            return new ProductServiceCacheable(azureBlobClient, filepathProduct);
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
}
