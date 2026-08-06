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
public class UserAttachmentsBlobStorageReadinessCheck extends AbstractBlobStorageReadinessCheck {

    static final String READINESS_PROBE_PREFIX = "__healthcheck_probe__/";
    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClient blobClient;
    private final String container;
    private final String account;

    @Inject
    public UserAttachmentsBlobStorageReadinessCheck(
            StorageRegistry storageRegistry,
            @ConfigProperty(name = "document-ms.blob-storage.container-user") String container,
            @ConfigProperty(name = "document-ms.blob-storage.account-name-user") Optional<String> account) {
        this.blobClient = storageRegistry.clientFor(StorageOrigin.USER);
        this.container = container;
        this.account = account.filter(s -> !s.isBlank()).orElse(ACCOUNT_NOT_APPLICABLE);
    }

    @Override
    protected String checkName() {
        return "blob-storage-user-attachments";
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
        return READINESS_PROBE_PREFIX;
    }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .item(() -> blobClient.getFiles(READINESS_PROBE_PREFIX))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
