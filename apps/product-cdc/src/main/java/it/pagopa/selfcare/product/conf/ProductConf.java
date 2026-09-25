package it.pagopa.selfcare.product.conf;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
@Data
public class ProductConf {
  @ApplicationScoped
  public TelemetryClient telemetryClient(
      @ConfigProperty(name = "product-cdc.appinsights.connection-string")
          String appInsightsConnectionString) {
    ConnectionString.configure(appInsightsConnectionString);
    return new TelemetryClient();
  }

  @ApplicationScoped
  public TableClient tableClient(
      @ConfigProperty(name = "product-cdc.storage.connection-string")
          String storageConnectionString,
      @ConfigProperty(name = "product-cdc.table.name") String tableName) {
    return new TableClientBuilder()
        .connectionString(storageConnectionString)
        .tableName(tableName)
        .buildClient();
  }
}
