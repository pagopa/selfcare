package it.pagopa.selfcare.user.event;

import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class UserGroupCdcLifecycle {
    @ConfigProperty(name = "user-group-cdc.storage.connection-string") String storageConnectionString;
    @ConfigProperty(name = "user-group-cdc.table.name") String tableName;


    void onStart(@Observes StartupEvent ev) {

        if(ConfigUtils.getProfiles().contains("test")) {
            //Not perform any action when testing
            return;
        }

        log.info("The application is starting...");

        // Table CdCStartAt will be created
        TableServiceClient tableServiceClient = new TableServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
        tableServiceClient.createTableIfNotExists(tableName);
    }
}
