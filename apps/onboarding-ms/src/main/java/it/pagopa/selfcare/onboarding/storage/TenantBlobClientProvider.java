package it.pagopa.selfcare.onboarding.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TenantBlobClientProvider {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private final ConcurrentHashMap<ClientKey, AzureBlobClient> clients = new ConcurrentHashMap<>();

    @Inject
    public TenantBlobClientProvider(TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @PostConstruct
    void initialize() {
        tenantRegistry.supportedTenantIds()
                .forEach(tenantId -> tenantRegistry.mandatoryStorageKeys()
                        .forEach(logicalKey -> clientFor(tenantId, logicalKey)));
    }

    public AzureBlobClient clientForCurrentTenant(String logicalStorageKey) {
        return clientFor(tenantContext.requiredTenantId(), logicalStorageKey);
    }

    public AzureBlobClient clientFor(String tenantId, String logicalStorageKey) {
        TenantDefinition.StorageDefinition storage = tenantRegistry.storage(tenantId, logicalStorageKey);
        AzureBlobClient client = clients.computeIfAbsent(clientKey(storage), key -> createClient(tenantId, logicalStorageKey, storage));
        return PrefixingAzureBlobClient.wrap(client, storage.pathPrefix());
    }

    protected AzureBlobClient createClient(
            String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
        TenantDefinition.StorageAuthentication authentication = storage.authentication();
        return switch (authentication.type()) {
            case CONNECTION_STRING -> {
                String connectionString = tenantRegistry.storageConnectionString(tenantId, logicalStorageKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing storage connection string for tenant "
                                        + tenantId
                                        + " and key "
                                        + logicalStorageKey));
                log.info("Creating Azure Blob client with connection string: tenant={}, key={}, container={}",
                        tenantId, logicalStorageKey, storage.container());
                yield new AzureBlobClientDefault(connectionString, storage.container());
            }
            case MANAGED_IDENTITY -> {
                String managedIdentityClientId = tenantRegistry
                        .storageManagedIdentityClientId(tenantId, logicalStorageKey)
                        .orElse("");
                log.info("Creating Azure Blob client with managed identity: tenant={}, key={}, account={}, container={}, managedIdentityClientIdConfigured={}",
                        tenantId,
                        logicalStorageKey,
                        storage.account(),
                        storage.container(),
                        !managedIdentityClientId.isBlank());
                yield new AzureBlobClientDefault(storage.container(), storage.account(), managedIdentityClientId);
            }
        };
    }

    private static ClientKey clientKey(TenantDefinition.StorageDefinition storage) {
        TenantDefinition.StorageAuthentication authentication = storage.authentication();
        String credentialRef = switch (authentication.type()) {
            case CONNECTION_STRING -> authentication.connectionStringEnvVar();
            case MANAGED_IDENTITY -> Objects.toString(authentication.managedIdentityClientIdEnvVar(), "");
        };
        return new ClientKey(storage.account(), storage.container(), authentication.type().name(), credentialRef);
    }

    private record ClientKey(String account, String container, String authenticationType, String credentialRef) {
    }
}
