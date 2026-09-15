package it.pagopa.selfcare.user.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.health.AbstractBlobStorageReadinessCheck;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

import java.util.Optional;

import static it.pagopa.selfcare.user.constant.TemplateMailConstant.ACTIVATE_TEMPLATE;

@Readiness
@ApplicationScoped
public class TemplatesBlobStorageReadinessCheck extends AbstractBlobStorageReadinessCheck {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final AzureBlobClient blobClient;
    private final String container;
    private final String account;
    private final String probeTarget;

    @Inject
    public TemplatesBlobStorageReadinessCheck(
            AzureBlobClient templatesBlobClient,
            @ConfigProperty(name = "user-ms.blob-storage.container-templates") String container,
            @ConfigProperty(name = "user-ms.blob-storage.account-name-templates") Optional<String> account,
            @ConfigProperty(name = "user-ms.blob-storage.filepath-templates") String templatesPath) {
        this.blobClient = templatesBlobClient;
        this.container = container;
        this.account = account.filter(s -> !s.isBlank()).orElse(ACCOUNT_NOT_APPLICABLE);
        this.probeTarget = templatesPath + ACTIVATE_TEMPLATE;
    }

    @Override
    protected String checkName() {
        return "blob-storage-templates";
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
