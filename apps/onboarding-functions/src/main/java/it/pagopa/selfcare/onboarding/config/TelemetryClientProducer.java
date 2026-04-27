package it.pagopa.selfcare.onboarding.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TelemetryClientProducer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryClientProducer.class);

    @Produces
    @Singleton
    public TelemetryClient telemetryClient(
            @ConfigProperty(name = "onboarding-functions.appinsights.connection-string")
            String appInsightsConnectionString,
            @ConfigProperty(name = "APPLICATIONINSIGHTS_ROLE_NAME", defaultValue = "onboarding-fn")
            String roleName) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        telemetryConfiguration.setRoleName(roleName);

        if (appInsightsConnectionString == null || appInsightsConnectionString.contains("00000000-0000-0000-0000-000000000000")) {
            log.warn("Application Insights connection string is using the fallback placeholder; telemetry will not be visible in the target resource");
        }

        return new TelemetryClient(telemetryConfiguration);
    }
}
