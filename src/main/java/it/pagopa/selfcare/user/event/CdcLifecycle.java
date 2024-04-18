package it.pagopa.selfcare.user.event;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.TableEntity;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.selfcare.user.event.constant.CdcStartAtConstant.*;

@Slf4j
@ApplicationScoped
public class CdcLifecycle {
    
    @ConfigProperty(name = "user-cdc.storage.connection-string") String storageConnectionString;
    @ConfigProperty(name = "user-cdc.table.name") String tableName;


    void onStart(@Observes StartupEvent ev) {
        log.info("The application is starting...");

        // Table CdCStartAt will be created
        TableServiceClient tableServiceClient = new TableServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
        tableServiceClient.createTableIfNotExists(tableName);
    }
}
