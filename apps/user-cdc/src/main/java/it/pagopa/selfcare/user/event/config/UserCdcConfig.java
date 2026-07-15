package it.pagopa.selfcare.user.event.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class UserCdcConfig {

    @ConfigProperty(name = "user-cdc.table.name")
    String tableName;

    @ConfigProperty(name = "user-cdc.table.connection-string")
    Optional<String> tableConnectionString;

    @ConfigProperty(name = "user-cdc.table.account-name")
    Optional<String> tableAccountName;

    @ConfigProperty(name = "user-cdc.table.managed-identity-client-id")
    Optional<String> tableManagedIdentityClientId;

    @ConfigProperty(name = "user-cdc.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "user-cdc.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "user-cdc.storage.connection-string")
    Optional<String> storageConnectionString;

    @ConfigProperty(name = "user-cdc.storage.account-name")
    Optional<String> storageAccountName;

    @ConfigProperty(name = "user-cdc.storage.managed-identity-client-id")
    Optional<String> storageManagedIdentityClientId;

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "user-cdc.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

    @ApplicationScoped
    public TableClient tableClient() {
        return tableConnectionString
          .filter(cs -> !cs.isBlank())
          .map(cs -> new TableClientBuilder().connectionString(cs).tableName(tableName).buildClient())
          .orElseGet(() -> new TableClientBuilder().endpoint("https://" + tableAccountName.orElse("") + ".table.core.windows.net")
            .credential(new DefaultAzureCredentialBuilder().managedIdentityClientId(tableManagedIdentityClientId.orElse("")).build())
            .tableName(tableName).buildClient());
    }

    @ApplicationScoped
    public ProductService productService() {
        return storageConnectionString
          .filter(cs -> !cs.isBlank())
          .map(cs -> new ProductServiceCacheable(cs, containerProduct, filepathProduct))
          .orElseGet(() -> new ProductServiceCacheable(containerProduct, filepathProduct,
            storageAccountName.orElse(""), storageManagedIdentityClientId.orElse("")));
    }

}
