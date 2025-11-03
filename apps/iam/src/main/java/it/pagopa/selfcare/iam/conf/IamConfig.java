package it.pagopa.selfcare.iam.conf;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IamConfig {

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "iam-ms.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

}
