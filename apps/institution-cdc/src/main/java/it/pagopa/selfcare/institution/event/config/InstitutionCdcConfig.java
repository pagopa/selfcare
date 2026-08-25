package it.pagopa.selfcare.institution.event.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class InstitutionCdcConfig {

    @ApplicationScoped
    public TableClient tableClient(@ConfigProperty(name = "institution-cdc.storage.connection-string") Optional<String> storageConnectionString,
                                   @ConfigProperty(name = "institution-cdc.table.name") String tableName,
                                   @ConfigProperty(name = "institution-cdc.storage-account-name") Optional<String> storageAccountName,
                                   @ConfigProperty(name = "institution-cdc.managed-identity-client-id") Optional<String> managedIdentityClientId){
        return storageConnectionString
          .filter(cs -> !cs.isBlank())
          .map(cs -> new TableClientBuilder().connectionString(cs).tableName(tableName).buildClient())
          .orElseGet(() -> new TableClientBuilder().endpoint("https://" + storageAccountName.orElse("") + ".table.core.windows.net")
            .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(managedIdentityClientId.orElse("")).build())
            .tableName(tableName).buildClient());
    }

}

