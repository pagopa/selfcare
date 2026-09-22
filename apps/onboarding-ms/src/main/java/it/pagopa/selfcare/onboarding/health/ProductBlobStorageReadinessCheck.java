package it.pagopa.selfcare.onboarding.health;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import it.pagopa.selfcare.commons.health.HealthCheckConstants;
import it.pagopa.selfcare.onboarding.storage.StorageKeys;
import it.pagopa.selfcare.onboarding.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Readiness
@ApplicationScoped
public class ProductBlobStorageReadinessCheck extends AbstractAsyncReadinessCheck {

    private final TenantRegistry tenantRegistry;
    private final TenantBlobClientProvider blobClientProvider;
    private final String probeTarget;

    @Inject
    public ProductBlobStorageReadinessCheck(
            TenantRegistry tenantRegistry,
            TenantBlobClientProvider blobClientProvider,
            @ConfigProperty(name = "onboarding-ms.blob-storage.filepath-product") String probeTarget) {
        this.tenantRegistry = tenantRegistry;
        this.blobClientProvider = blobClientProvider;
        this.probeTarget = probeTarget;
    }

    @Override
    protected String checkName() {
        return "blob-storage-product";
    }

    @Override
    protected Map<String, String> data() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(HealthCheckConstants.DATA_KEY_COMPONENT, "blob-storage");
        data.put("logicalKey", StorageKeys.PRODUCTS);
        data.put("tenants", String.join(",", tenantRegistry.supportedTenantIds()));
        data.put(HealthCheckConstants.DATA_KEY_BLOB_PROBE_TARGET, probeTarget);
        String accounts = tenantRegistry.supportedTenantIds().stream()
                .map(tenantId -> tenantRegistry.storage(tenantId, StorageKeys.PRODUCTS))
                .map(TenantDefinition.StorageDefinition::account)
                .collect(Collectors.joining(","));
        String containers = tenantRegistry.supportedTenantIds().stream()
                .map(tenantId -> tenantRegistry.storage(tenantId, StorageKeys.PRODUCTS).container())
                .distinct()
                .collect(Collectors.joining(","));
        data.put(HealthCheckConstants.DATA_KEY_BLOB_ACCOUNT, accounts);
        data.put(HealthCheckConstants.DATA_KEY_BLOB_CONTAINER, containers);
        return data;
    }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .item(() -> {
                    tenantRegistry.supportedTenantIds().forEach(tenantId ->
                            blobClientProvider.clientFor(tenantId, StorageKeys.PRODUCTS).getProperties(probeTarget));
                    return true;
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
