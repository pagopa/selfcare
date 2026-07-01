package it.pagopa.selfcare.user.conf;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import it.pagopa.selfcare.user.auth.EventhubSasTokenAuthorization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class UserMsConfig {

    @ConfigProperty(name = "user-ms.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "user-ms.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "user-ms.blob-storage.connection-string-product")
    Optional<String> connectionStringProduct;

    @ConfigProperty(name = "user-ms.blob-storage.account-name-product")
    Optional<String> accountNameProduct;

    @ConfigProperty(name = "user-ms.blob-storage.managed-identity-client-id-product")
    Optional<String> managedIdentityClientIdProduct;

    @ConfigProperty(name = "user-ms.blob-storage.connection-string-templates")
    Optional<String> connectionStringTemplates;

    @ConfigProperty(name = "user-ms.blob-storage.account-name-templates")
    Optional<String> accountNameTemplates;

    @ConfigProperty(name = "user-ms.blob-storage.managed-identity-client-id-templates")
    Optional<String> managedIdentityClientIdTemplates;

    @ConfigProperty(name = "user-ms.blob-storage.container-templates")
    String containerTemplates;

    @ApplicationScoped
    public ProductService productService(){
        return connectionStringProduct
          .filter(cs -> !cs.isBlank())
          .map(cs -> new ProductServiceCacheable(cs, containerProduct, filepathProduct))
          .orElseGet(() -> new ProductServiceCacheable(containerProduct, filepathProduct,
            accountNameProduct.orElse(""), managedIdentityClientIdProduct.orElse("")));
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract() {
        return connectionStringTemplates
          .filter(cs -> !cs.isBlank())
          .map(cs -> new AzureBlobClientDefault(cs, containerTemplates))
          .orElseGet(() -> new AzureBlobClientDefault(containerTemplates,
            accountNameTemplates.orElse(""), managedIdentityClientIdTemplates.orElse("")));
    }

    @Produces
    @ApplicationScoped
    public EventhubSasTokenAuthorization eventhubSasTokenAuthorization(){
        return new EventhubSasTokenAuthorization();

    }

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "user-ms.appinsights.connection-string") String appInsightsConnectionString) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        return new TelemetryClient(telemetryConfiguration);
    }

}
