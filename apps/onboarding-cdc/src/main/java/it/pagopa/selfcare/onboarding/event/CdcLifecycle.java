package it.pagopa.selfcare.onboarding.event;

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

  @ConfigProperty(name = "onboarding-cdc.storage.connection-string")
  Optional<String> storageConnectionString;
  @ConfigProperty(name = "onboarding-cdc.table.name")
  String tableName;
  @ConfigProperty(name = "onboarding-cdc.storage-account-name")
  Optional<String> storageAccountName;
  @ConfigProperty(name = "onboarding-cdc.managed-identity-client-id")
  Optional<String> managedIdentityClientId;


  void onStart(@Observes StartupEvent ev) {

    if (ConfigUtils.getProfiles().contains("test")) {
      //Not perform any action when testing
      return;
    }

    log.info("The application is starting...");
    buildTableServiceClient().createTableIfNotExists(tableName);
  }

  TableServiceClient buildTableServiceClient() {
    return storageConnectionString
      .filter(cs -> !cs.isBlank())
      .map(cs -> {
        log.info("Azure Table Storage: using connection string");
        return new TableServiceClientBuilder().connectionString(cs).buildClient();
      })
      .orElseGet(() -> {
        log.info("Azure Table Storage: using managed identity (clientId: {})", managedIdentityClientId.orElse("default"));
        return new TableServiceClientBuilder().endpoint("https://" + storageAccountName.orElse("") + ".table.core.windows.net")
          .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId.orElse("")).build()).buildClient();
      });
  }
}
