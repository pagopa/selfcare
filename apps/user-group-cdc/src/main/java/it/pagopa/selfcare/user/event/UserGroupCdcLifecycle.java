package it.pagopa.selfcare.user.event;

import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class UserGroupCdcLifecycle {
    @ConfigProperty(name = "user-group-cdc.storage.connection-string") Optional<String> storageConnectionString;
    @ConfigProperty(name = "user-group-cdc.table.name") String tableName;
    @ConfigProperty(name = "user-group-cdc.storage-account-name") Optional<String> storageAccountName;
    @ConfigProperty(name = "user-group-cdc.storage-client-id") Optional<String> managedIdentityClientId;

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
