package it.pagopa.selfcare.user.event.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class UserGroupCdcConfig {

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "user-group-cdc.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

    @ApplicationScoped
    public TableClient tableClient(@ConfigProperty(name = "user-group-cdc.storage.connection-string") Optional<String> storageConnectionString,
                                   @ConfigProperty(name = "user-group-cdc.table.name") String tableName,
                                   @ConfigProperty(name = "user-group-cdc.storage-account-name") Optional<String> storageAccountName,
                                   @ConfigProperty(name = "user-group-cdc.storage-client-id") Optional<String> managedIdentityClientId) {
        return storageConnectionString
            .filter(cs -> !cs.isBlank())
            .map(cs -> new TableClientBuilder().connectionString(cs).tableName(tableName).buildClient())
            .orElseGet(() -> new TableClientBuilder().endpoint("https://" + storageAccountName.orElse("") + ".table.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId.orElse("")).build())
                .tableName(tableName).buildClient());
    }

}
