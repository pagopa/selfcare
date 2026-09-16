package it.pagopa.selfcare.user.conf;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
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

    @ApplicationScoped
    public ProductService productService(AzureBlobClientDefault productBlobClient) {
        return new ProductServiceCacheable(productBlobClient, filepathProduct);
    }

    @ApplicationScoped
    @Typed(AzureBlobClientDefault.class)
    public AzureBlobClientDefault productBlobClient() {
        return connectionStringProduct
          .filter(cs -> !cs.isBlank())
          .map(cs -> new AzureBlobClientDefault(cs, containerProduct))
          .orElseGet(() -> new AzureBlobClientDefault(containerProduct,
            accountNameProduct.orElse(""), managedIdentityClientIdProduct.orElse("")));
    }

    @ApplicationScoped
    public TelemetryClient telemetryClient(@ConfigProperty(name = "user-ms.appinsights.connection-string") String appInsightsConnectionString) {
        ConnectionString.configure(appInsightsConnectionString);
        return new TelemetryClient();
    }

}
