package it.pagopa.selfcare.iam.conf;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IamConfig {

  @ApplicationScoped
  public TelemetryClient telemetryClient(
      @ConfigProperty(name = "iam-ms.appinsights.connection-string")
          String appInsightsConnectionString) {
    ConnectionString.configure(appInsightsConnectionString);
    return new TelemetryClient();
  }
}
