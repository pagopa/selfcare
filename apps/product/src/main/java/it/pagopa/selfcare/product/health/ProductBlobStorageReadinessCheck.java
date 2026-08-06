package it.pagopa.selfcare.product.health;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractBlobStorageReadinessCheck;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ProductBlobStorageReadinessCheck extends AbstractBlobStorageReadinessCheck {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final BlobServiceAsyncClient blobClient;
    private final String container;
    private final String account;

    @Inject
    public ProductBlobStorageReadinessCheck(
            @ConfigProperty(name = "product-ms.blob-storage.container-contract-template") String container,
            @ConfigProperty(name = "product-ms.blob-storage.connection-string-contract-template") String connectionString) {
        this.container = container;
        this.blobClient = new BlobServiceClientBuilder().connectionString(connectionString).buildAsyncClient();
        // account name is not directly exposed by the connection-string flow: it lives inside the
        // connection string itself. We keep the field as informational metadata; if the app ever
        // migrates to Managed Identity a new @ConfigProperty for the account name should be wired here.
        this.account = ACCOUNT_NOT_APPLICABLE;
    }

    /** Test-only constructor allowing an already-built client to be injected. */
    ProductBlobStorageReadinessCheck(String container, BlobServiceAsyncClient blobClient, String account) {
        this.container = container;
        this.blobClient = blobClient;
        this.account = account;
    }

    @Override
    protected String checkName() {
        return "blob-storage-contract-template";
    }

    @Override
    protected String account() {
        return account;
    }

    @Override
    protected String container() {
        return container;
    }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .completionStage(
                        blobClient.getBlobContainerAsyncClient(container).getProperties().toFuture());
    }
}

