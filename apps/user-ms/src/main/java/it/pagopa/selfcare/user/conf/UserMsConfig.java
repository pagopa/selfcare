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

@ApplicationScoped
public class UserMsConfig {

    @ConfigProperty(name = "user-ms.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "user-ms.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "user-ms.blob-storage.connection-string-product")
    String connectionStringProduct;

    @ConfigProperty(name = "user-ms.blob-storage.connection-string-templates")
    String connectionStringTemplates;

    @ConfigProperty(name = "user-ms.blob-storage.container-templates")
    String containerTemplates;

    @ApplicationScoped
    public ProductService productService(){
        AzureBlobClient azureBlobClient = new AzureBlobClientDefault(connectionStringProduct, containerProduct);
        try{
            return new ProductServiceCacheable(azureBlobClient, filepathProduct);
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Found an issue when trying to serialize product json string!!");
        }
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract(){
        return new AzureBlobClientDefault(connectionStringTemplates, containerTemplates);
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
