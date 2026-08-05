package it.pagopa.selfcare.onboarding.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TelemetryClientProducer {

    @Produces
    public TelemetryClient telemetryClient(
            @ConfigProperty(name = "onboarding-functions.appinsights.connection-string")
            String appInsightsConnectionString) {
        ConnectionString.configure(appInsightsConnectionString);
        return new TelemetryClient();
    }
}
