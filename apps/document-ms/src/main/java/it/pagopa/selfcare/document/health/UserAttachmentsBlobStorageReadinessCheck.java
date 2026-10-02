package it.pagopa.selfcare.document.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.health.AbstractBlobStorageReadinessCheck;
import it.pagopa.selfcare.document.storage.StorageKeys;
import it.pagopa.selfcare.document.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.Readiness;

import java.util.Comparator;

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
            TenantBlobClientProvider blobClientProvider,
            TenantRegistry tenantRegistry) {
        String tenantId = tenantRegistry.supportedTenantIds().stream()
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tenant configured for storage readiness"));
        TenantDefinition.StorageDefinition storage = tenantRegistry.storage(tenantId, StorageKeys.USER_ATTACHMENTS);
        this.blobClient = blobClientProvider.clientFor(tenantId, StorageKeys.USER_ATTACHMENTS);
        this.container = storage.container();
        this.account = storage.account() == null || storage.account().isBlank() ? ACCOUNT_NOT_APPLICABLE : storage.account();
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
