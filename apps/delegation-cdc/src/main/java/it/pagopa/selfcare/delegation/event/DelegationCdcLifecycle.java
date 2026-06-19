package it.pagopa.selfcare.delegation.event;

import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class DelegationCdcLifecycle {
    @ConfigProperty(name = "delegation-cdc.storage.connection-string") Optional<String> storageConnectionString;
    @ConfigProperty(name = "delegation-cdc.table.name") String tableName;
    @ConfigProperty(name = "delegation-cdc.storage-account-name") Optional<String> storageAccountName;
    @ConfigProperty(name = "delegation-cdc.managed-identity-client-id") Optional<String> managedIdentityClientId;


    void onStart(@Observes StartupEvent ev) {

        if(ConfigUtils.getProfiles().contains("test")) {
            //Not perform any action when testing
            return;
        }

        log.info("The application is starting...");

        // Table CdCStartAt will be created
        final TableServiceClient tableServiceClient = storageConnectionString
            .filter(cs -> !cs.isBlank())
            .map(cs -> new TableServiceClientBuilder().connectionString(cs).buildClient())
            .orElseGet(() -> new TableServiceClientBuilder().endpoint("https://" + storageAccountName.orElse("") + ".table.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId.orElse("")).build()).buildClient());
        tableServiceClient.createTableIfNotExists(tableName);
    }
}
