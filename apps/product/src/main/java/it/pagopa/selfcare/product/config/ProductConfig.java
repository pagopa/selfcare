package it.pagopa.selfcare.product.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ProductConfig {

  @ApplicationScoped
  public TelemetryClient telemetryClient(
      @ConfigProperty(name = "product-ms.appinsights.connection-string")
          String appInsightsConnectionString) {
    ConnectionString.configure(appInsightsConnectionString);
    return new TelemetryClient();
  }
}
