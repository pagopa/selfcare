package it.pagopa.selfcare.user.event.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserCdcConfig {

    @ConfigProperty(name = "user-cdc.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "user-cdc.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "user-cdc.storage.connection-string")
    String connectionStringProduct;

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "user-cdc.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

    @ApplicationScoped
    public TableClient tableClient(@ConfigProperty(name = "user-cdc.storage.connection-string") String storageConnectionString,
                                   @ConfigProperty(name = "user-cdc.table.name") String tableName){
        return new TableClientBuilder()
                .connectionString(storageConnectionString)
                .tableName(tableName)
                .buildClient();
    }

    @ApplicationScoped
    public ProductService productService() {
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(connectionStringProduct, containerProduct);
        try{
            return new ProductServiceCacheable(azureBlobClient, filepathProduct);
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }

}
