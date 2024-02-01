package it.pagopa.selfcare.user.event.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TelemetryClientConfig {

    @ConfigProperty(name = "user-cdc.appinsights.connection-string")
    String appInsightsConnectionString;

    @ApplicationScoped
    public TelemetryClient telemetryClient() {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

}
