package it.pagopa.selfcare.user.conf;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserMsConfig {

    @ConfigProperty(name = "user-ms.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "user-ms.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "user-ms.blob-storage.connection-string-product")
    String connectionStringProduct;

    @ApplicationScoped
    public ProductService productService(){
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(connectionStringProduct, containerProduct);
        try{
            return new ProductServiceCacheable(azureBlobClient, filepathProduct);
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
}
