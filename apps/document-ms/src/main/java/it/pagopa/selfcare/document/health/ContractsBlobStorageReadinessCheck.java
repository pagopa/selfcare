package it.pagopa.selfcare.document.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.health.AbstractBlobStorageReadinessCheck;
import it.pagopa.selfcare.document.config.StorageRegistry;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

import java.util.Optional;

@Readiness
@ApplicationScoped
public class ContractsBlobStorageReadinessCheck extends AbstractBlobStorageReadinessCheck {

    static final String READINESS_PROBE_PREFIX = "__healthcheck_probe__/";
    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClient blobClient;
    private final String container;
    private final String account;

    @Inject
    public ContractsBlobStorageReadinessCheck(
            StorageRegistry storageRegistry,
            @ConfigProperty(name = "document-ms.blob-storage.container-contracts") String container,
            @ConfigProperty(name = "document-ms.blob-storage.account-name-contracts") Optional<String> account) {
        this.blobClient = storageRegistry.clientFor(StorageOrigin.SYSTEM);
        this.container = container;
        this.account = account.filter(s -> !s.isBlank()).orElse(ACCOUNT_NOT_APPLICABLE);
    }

    @Override
    protected String checkName() {
        return "blob-storage-contracts";
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
    protected String canaryBlob() {
        return READINESS_PROBE_PREFIX;
    }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .item(() -> blobClient.getFiles(READINESS_PROBE_PREFIX))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}

