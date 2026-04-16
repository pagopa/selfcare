package it.pagopa.selfcare.user.event.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserGroupCdcConfig {

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "user-group-cdc.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

    @ApplicationScoped
    public TableClient tableClient(@ConfigProperty(name = "user-group-cdc.storage.connection-string") String storageConnectionString,
                                   @ConfigProperty(name = "user-group-cdc.table.name") String tableName){
        return new TableClientBuilder()
                .connectionString(storageConnectionString)
                .tableName(tableName)
                .buildClient();
    }

}
