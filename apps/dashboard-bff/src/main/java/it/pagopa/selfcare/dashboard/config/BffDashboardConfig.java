package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BffDashboardConfig {

    private final DashboardConfig config;

    @Bean
    public ProductService productService(){
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(config.getBlobStorage().getConnectionStringProduct(), config.getBlobStorage().getContainerProduct());
        try{
            return new ProductServiceCacheable(azureBlobClient, config.getBlobStorage().getFilepathProduct());
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }
}
