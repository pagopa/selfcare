package it.pagopa.selfcare.document.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import it.pagopa.selfcare.commons.health.HealthCheckConstants;
import it.pagopa.selfcare.document.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.Readiness;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Readiness
@ApplicationScoped
public class TenantBlobStorageReadinessCheck extends AbstractAsyncReadinessCheck {

    static final String READINESS_PROBE_PREFIX = "__healthcheck_probe__/";

    private final TenantBlobClientProvider blobClientProvider;
    private final List<TenantStorageTarget> targets;
    private final String accounts;
    private final String containers;

    @Inject
    public TenantBlobStorageReadinessCheck(
            TenantRegistry tenantRegistry,
            TenantBlobClientProvider blobClientProvider) {
        this.blobClientProvider = blobClientProvider;
        this.targets = tenantRegistry.supportedTenantIds().stream()
                .sorted()
                .flatMap(tenantId -> tenantRegistry.mandatoryStorageKeys().stream()
                        .sorted()
                        .map(logicalKey -> target(tenantRegistry, tenantId, logicalKey)))
                .toList();
        this.accounts = joinTargets(TenantStorageTarget::account);
        this.containers = joinTargets(TenantStorageTarget::container);
    }

    @Override
    protected String checkName() {
        return "blob-storage-document";
    }

    @Override
    protected Map<String, String> data() {
        return Map.of(
                HealthCheckConstants.DATA_KEY_COMPONENT, "blob-storage",
                HealthCheckConstants.DATA_KEY_BLOB_ACCOUNT, accounts,
                HealthCheckConstants.DATA_KEY_BLOB_CONTAINER, containers,
                HealthCheckConstants.DATA_KEY_BLOB_PROBE_TARGET, READINESS_PROBE_PREFIX);
    }

    @Override
    protected Uni<?> probe() {
        if (targets.isEmpty()) {
            return Uni.createFrom().failure(new IllegalStateException("No mandatory tenant storage configured"));
        }
        List<Uni<List<String>>> probes = targets.stream()
                .map(this::probe)
                .toList();
        return Uni.join().all(probes).andFailFast();
    }

    private Uni<List<String>> probe(TenantStorageTarget target) {
        return Uni.createFrom()
                .item(() -> {
                    AzureBlobClient client = blobClientProvider.clientFor(target.tenantId(), target.logicalKey());
                    return client.getFiles(READINESS_PROBE_PREFIX);
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onFailure().transform(failure -> new IllegalStateException(
                        "Tenant " + target.tenantId() + " storage " + target.logicalKey() + " probe failed: "
                                + failure.getClass().getSimpleName() + ": " + failure.getMessage(),
                        failure));
    }

    private TenantStorageTarget target(TenantRegistry tenantRegistry, String tenantId, String logicalKey) {
        TenantDefinition.StorageDefinition storage = tenantRegistry.storage(tenantId, logicalKey);
        return new TenantStorageTarget(tenantId, logicalKey, storage.account(), storage.container());
    }

    private String joinTargets(java.util.function.Function<TenantStorageTarget, String> value) {
        if (targets.isEmpty()) {
            return "n/a";
        }
        return targets.stream()
                .sorted(Comparator.comparing(TenantStorageTarget::tenantId).thenComparing(TenantStorageTarget::logicalKey))
                .map(t -> t.tenantId() + ":" + t.logicalKey() + "=" + value.apply(t))
                .collect(Collectors.joining(","));
    }

    private record TenantStorageTarget(String tenantId, String logicalKey, String account, String container) {
    }
}
