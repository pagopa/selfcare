package it.pagopa.selfcare.onboarding.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.commons.health.AbstractBlobStorageReadinessCheck;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

import java.util.Optional;

@Readiness
@ApplicationScoped
public class ProductBlobStorageReadinessCheck extends AbstractBlobStorageReadinessCheck {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClientDefault blobClient;
    private final String container;
    private final String account;
    private final String probeTarget;

    @Inject
    public ProductBlobStorageReadinessCheck(
            AzureBlobClientDefault productBlobClient,
            @ConfigProperty(name = "onboarding-ms.blob-storage.container-product") String container,
            @ConfigProperty(name = "onboarding-ms.blob-storage.account-name-product") Optional<String> account,
            @ConfigProperty(name = "onboarding-ms.blob-storage.filepath-product") String probeTarget) {
        this.blobClient = productBlobClient;
        this.container = container;
        this.account = account.filter(s -> !s.isBlank()).orElse(ACCOUNT_NOT_APPLICABLE);
        this.probeTarget = probeTarget;
    }

    @Override
    protected String checkName() {
        return "blob-storage-product";
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
    protected String probeTarget() {
        return probeTarget;
    }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .item(() -> blobClient.getProperties(probeTarget))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
