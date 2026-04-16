package it.pagopa.selfcare.institution.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceDefault;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class InstitutionSendMailConfig {

    @Produces
    public ObjectMapper objectMapper(){
        ObjectMapper mapper =  DatabindCodec.mapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);// custom config
        mapper.registerModule(new JavaTimeModule());                               // custom config
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);            // custom config
        return mapper;
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract(AzureStorageConfig azureStorageConfig){
        return new AzureBlobClientDefault(azureStorageConfig.connectionStringContract(), azureStorageConfig.containerContract());
    }

    @ApplicationScoped
    public ProductService productService(AzureStorageConfig azureStorageConfig){
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(azureStorageConfig.connectionStringProduct(), azureStorageConfig.containerProduct());
        String productJsonString = azureBlobClient.getFileAsText(azureStorageConfig.productFilepath());
        try {
            return new ProductServiceDefault(productJsonString, objectMapper());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
}
