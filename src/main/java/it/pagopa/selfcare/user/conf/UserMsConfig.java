package it.pagopa.selfcare.user.conf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceDefault;
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
    public ProductService productService(ObjectMapper objectMapper){
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(connectionStringProduct, containerProduct);
        String productJsonString = azureBlobClient.getFileAsText(filepathProduct);
        try {
            return new ProductServiceDefault(productJsonString, objectMapper);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
}
