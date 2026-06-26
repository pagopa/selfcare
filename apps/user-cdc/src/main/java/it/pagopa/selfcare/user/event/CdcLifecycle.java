package it.pagopa.selfcare.user.event;

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
public class CdcLifecycle {

    @ConfigProperty(name = "user-cdc.table.name") String tableName;
    @ConfigProperty(name = "user-cdc.table.connection-string") Optional<String> tableConnectionString;
    @ConfigProperty(name = "user-cdc.table.account-name") Optional<String> tableAccountName;
    @ConfigProperty(name = "user-cdc.table.managed-identity-client-id") Optional<String> tableManagedIdentityClientId;


    void onStart(@Observes StartupEvent ev) {

        if(ConfigUtils.getProfiles().contains("test")) {
            //Not perform any action when testing
            return;
        }

        log.info("The application is starting...");

        // Table CdCStartAt will be created
        final TableServiceClient tableServiceClient = tableConnectionString
          .filter(cs -> !cs.isBlank())
          .map(cs -> new TableServiceClientBuilder().connectionString(cs).buildClient())
          .orElseGet(() -> new TableServiceClientBuilder().endpoint("https://" + tableAccountName.orElse("") + ".table.core.windows.net")
            .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(tableManagedIdentityClientId.orElse("")).build()).buildClient());
        tableServiceClient.createTableIfNotExists(tableName);
    }
}
