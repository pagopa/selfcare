package it.pagopa.selfcare.auth.conf;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthConfig {

  @ApplicationScoped
  public TelemetryClient telemetryClient(
      @ConfigProperty(name = "auth-ms.appinsights.connection-string")
          String appInsightsConnectionString) {
    TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
    telemetryConfiguration.setConnectionString(appInsightsConnectionString);
    return new TelemetryClient(telemetryConfiguration);
  }
}
