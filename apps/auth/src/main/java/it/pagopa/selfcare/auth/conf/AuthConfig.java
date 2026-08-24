package it.pagopa.selfcare.auth.conf;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthConfig {

  @ApplicationScoped
  public TelemetryClient telemetryClient(
      @ConfigProperty(name = "auth-ms.appinsights.connection-string")
          String appInsightsConnectionString) {
    ConnectionString.configure(appInsightsConnectionString);
    return new TelemetryClient();
  }
}
