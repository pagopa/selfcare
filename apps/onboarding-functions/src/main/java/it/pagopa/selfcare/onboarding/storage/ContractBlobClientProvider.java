package it.pagopa.selfcare.onboarding.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition.StorageDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Resolves the contract/documents blob storage of the tenant in the current {@link TenantContext}
 * from the {@code contracts} storage of the {@link TenantRegistry}, i.e. the same account,
 * container and path prefix that {@code document-ms} uses. There is no fallback: a missing tenant
 * context or a tenant without the {@code contracts} storage fails closed.
 */
@ApplicationScoped
public class ContractBlobClientProvider {

    public static final String CONTRACTS_STORAGE_KEY = "contracts";

    private static final Logger log = LoggerFactory.getLogger(ContractBlobClientProvider.class);

    private final TenantRegistry registry;
    private final BiFunction<String, StorageDefinition, AzureBlobClient> delegateFactory;
    private final Map<String, AzureBlobClient> clients = new ConcurrentHashMap<>();

    @Inject
    public ContractBlobClientProvider(TenantRegistry registry) {
        this.registry = registry;
        this.delegateFactory = this::createDelegate;
    }

    ContractBlobClientProvider(
            TenantRegistry registry,
            BiFunction<String, StorageDefinition, AzureBlobClient> delegateFactory) {
        this.registry = registry;
        this.delegateFactory = delegateFactory;
    }

    public AzureBlobClient forCurrentTenant() {
        String tenant = TenantContext.requiredTenant();
        return clients.computeIfAbsent(tenant, this::createClient);
    }

    private AzureBlobClient createClient(String tenant) {
        StorageDefinition storage;
        try {
            storage = registry.storage(tenant, CONTRACTS_STORAGE_KEY);
        } catch (UnknownStorageException exception) {
            throw new IllegalStateException("No contract storage configured for tenant " + tenant, exception);
        }
        String pathPrefix = Objects.toString(storage.pathPrefix(), "");
        log.info("Contract blob storage client configured for tenant {}, container {}, prefixConfigured {}",
                tenant, storage.container(), !pathPrefix.isBlank());
        return new PrefixingAzureBlobClient(delegateFactory.apply(tenant, storage), pathPrefix);
    }

    private AzureBlobClient createDelegate(String tenant, StorageDefinition storage) {
        return switch (storage.authentication().type()) {
            case CONNECTION_STRING -> new AzureBlobClientDefault(
                    registry.storageConnectionString(tenant, CONTRACTS_STORAGE_KEY)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Missing contract storage connection string for tenant " + tenant)),
                    storage.container());
            case MANAGED_IDENTITY -> new AzureBlobClientDefault(
                    storage.container(),
                    storage.account(),
                    registry.storageManagedIdentityClientId(tenant, CONTRACTS_STORAGE_KEY).orElse(""));
        };
    }
}
