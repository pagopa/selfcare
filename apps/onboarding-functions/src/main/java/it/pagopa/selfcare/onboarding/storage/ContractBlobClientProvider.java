package it.pagopa.selfcare.onboarding.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.config.ContractStorageConfig;
import it.pagopa.selfcare.onboarding.config.ContractStorageConfig.TenantBinding;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Resolves the contract/documents blob storage of the tenant in the current {@link TenantContext},
 * i.e. the same account, container and path prefix that {@code document-ms} uses for its
 * {@code contracts} binding. There is no fallback: a missing tenant context or a tenant without a
 * binding fails closed.
 */
@ApplicationScoped
public class ContractBlobClientProvider {

    private static final Logger log = LoggerFactory.getLogger(ContractBlobClientProvider.class);

    private final ContractStorageConfig config;
    private final Function<TenantBinding, AzureBlobClient> delegateFactory;
    private final Map<String, AzureBlobClient> clients = new ConcurrentHashMap<>();

    @Inject
    public ContractBlobClientProvider(ContractStorageConfig config) {
        this(config, ContractBlobClientProvider::createDelegate);
    }

    ContractBlobClientProvider(
            ContractStorageConfig config, Function<TenantBinding, AzureBlobClient> delegateFactory) {
        this.config = config;
        this.delegateFactory = delegateFactory;
    }

    public AzureBlobClient forCurrentTenant() {
        String tenant = TenantContext.requiredTenant();
        return clients.computeIfAbsent(tenant, this::createClient);
    }

    private AzureBlobClient createClient(String tenant) {
        TenantBinding binding = Optional.ofNullable(config.tenants().get(tenant))
                .filter(ContractBlobClientProvider::isComplete)
                .orElseThrow(() -> new IllegalStateException(
                        "No contract storage binding configured for tenant " + tenant));
        log.info("Contract blob storage client configured for tenant {}, container {}, prefixConfigured {}",
                tenant, binding.container().orElse(""), hasText(binding.pathPrefix()));
        return new PrefixingAzureBlobClient(
                delegateFactory.apply(binding), binding.pathPrefix().orElse(""));
    }

    private static boolean isComplete(TenantBinding binding) {
        return hasText(binding.container())
                && (hasText(binding.connectionString()) || hasText(binding.accountName()));
    }

    private static boolean hasText(Optional<String> value) {
        return value.filter(text -> !text.isBlank()).isPresent();
    }

    private static AzureBlobClient createDelegate(TenantBinding binding) {
        String container = binding.container().orElseThrow();
        return binding.connectionString()
                .filter(connectionString -> !connectionString.isBlank())
                .<AzureBlobClient>map(connectionString -> new AzureBlobClientDefault(connectionString, container))
                .orElseGet(() -> new AzureBlobClientDefault(
                        container,
                        binding.accountName().orElse(""),
                        binding.managedIdentityClientId().orElse("")));
    }
}
