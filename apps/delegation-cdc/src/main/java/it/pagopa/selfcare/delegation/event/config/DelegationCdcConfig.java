package it.pagopa.selfcare.delegation.event.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class DelegationCdcConfig {

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "delegation-cdc.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

    @ApplicationScoped
    public TableClient tableClient(@ConfigProperty(name = "delegation-cdc.storage.connection-string") Optional<String> storageConnectionString,
                                   @ConfigProperty(name = "delegation-cdc.table.name") String tableName,
                                   @ConfigProperty(name = "delegation-cdc.storage-account-name") Optional<String> storageAccountName,
                                   @ConfigProperty(name = "delegation-cdc.managed-identity-client-id") Optional<String> managedIdentityClientId){
        return storageConnectionString
            .filter(cs -> !cs.isBlank())
            .map(cs -> new TableClientBuilder().connectionString(cs).tableName(tableName).buildClient())
            .orElseGet(() -> new TableClientBuilder().endpoint("https://" + storageAccountName.orElse("") + ".table.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId.orElse("")).build())
                .tableName(tableName).buildClient());
    }

}
