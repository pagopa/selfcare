package it.pagopa.selfcare.product.conf;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
@Data
public class ProductConf {
  @ConfigProperty(name = "product-cdc.blob-storage.container-product")
  String containerProduct;

  @ConfigProperty(name = "product-cdc.blob-storage.filepath-product")
  String filepathProduct;

  @ConfigProperty(name = "product-cdc.blob-storage.connection-string-product")
  String connectionStringProduct;

//  void onStart(@Observes StartupEvent ev) {
//    log.info(String.format("Database %s is starting...", Product.mongoDatabase().getName()));
//  }

  @ApplicationScoped
  public ProductService productService(){
    return new ProductServiceCacheable(connectionStringProduct, containerProduct, filepathProduct);
  }

  @ApplicationScoped
  public AzureBlobClient azureBobClientContract(){
    return new AzureBlobClientDefault(connectionStringProduct, containerProduct);
  }

  @ApplicationScoped
  public TelemetryClient telemetryClient(@ConfigProperty(name = "product-cdc.appinsights.connection-string") String appInsightsConnectionString) {
    TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
    telemetryConfiguration.setConnectionString(appInsightsConnectionString);
    return new TelemetryClient(telemetryConfiguration);
  }

  @ApplicationScoped
  public TableClient tableClient(@ConfigProperty(name = "product-cdc.storage.connection-string") String storageConnectionString,
                                 @ConfigProperty(name = "product-cdc.table.name") String tableName){
    return new TableClientBuilder()
      .connectionString(storageConnectionString)
      .tableName(tableName)
      .buildClient();
  }
}
