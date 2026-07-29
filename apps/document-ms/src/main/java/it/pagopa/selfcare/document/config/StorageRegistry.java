package it.pagopa.selfcare.document.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class StorageRegistry {

    @ConfigProperty(name = "document-ms.blob-storage.connection-string-contracts")
    Optional<String> systemConnectionString;

    @ConfigProperty(name = "document-ms.blob-storage.container-contracts")
    Optional<String> systemContainer;

    @ConfigProperty(name = "document-ms.blob-storage.account-name-contracts")
    Optional<String> systemAccountName;

    @ConfigProperty(name = "document-ms.blob-storage.connection-string-user")
    Optional<String> userConnectionString;

    @ConfigProperty(name = "document-ms.blob-storage.container-user")
    Optional<String> userContainer;

    @ConfigProperty(name = "document-ms.blob-storage.account-name-user")
    Optional<String> userAccountName;

    @ConfigProperty(name = "document-ms.blob-storage.managed-identity-client-id")
    Optional<String> managedIdentityClientId;

    private Map<StorageOrigin, AzureBlobClient> clientsByOrigin;

    @PostConstruct
    void init() {
        AzureBlobClient system = buildBlobClient(systemConnectionString, systemContainer, systemAccountName, StorageOrigin.SYSTEM)
                .orElseThrow(() -> new IllegalStateException("Missing required blob storage configuration for SYSTEM"));

        Map<StorageOrigin, AzureBlobClient> clients = new EnumMap<>(StorageOrigin.class);
        clients.put(StorageOrigin.SYSTEM, system);
        buildBlobClient(userConnectionString, userContainer, userAccountName, StorageOrigin.USER)
                .ifPresent(user -> clients.put(StorageOrigin.USER, user));

        this.clientsByOrigin = Map.copyOf(clients);
        log.info("StorageRegistry: initialized with {} configured client(s): {}", clientsByOrigin.size(), clientsByOrigin.keySet());
    }

    public AzureBlobClient clientFor(StorageOrigin storageOrigin) {
        StorageOrigin resolved = Objects.isNull(storageOrigin) ? StorageOrigin.SYSTEM : storageOrigin;
        log.debug("StorageRegistry: routing storageOrigin={}", resolved);
        return clientsByOrigin.getOrDefault(resolved, clientsByOrigin.get(StorageOrigin.SYSTEM));
    }

    private Optional<AzureBlobClient> buildBlobClient(Optional<String> connectionString, Optional<String> container,
                                                       Optional<String> accountName, StorageOrigin storageOrigin) {
        if (container.filter(s -> !s.isBlank()).isEmpty()) {
            return Optional.empty();
        }
        String containerValue = container.get();
        String clientId = managedIdentityClientId.filter(s -> !s.isBlank()).orElse(null);

        // Prefer managed identity authentication if account name is configured
        if (accountName.filter(s -> !s.isBlank()).isPresent()) {
            log.info("StorageRegistry: {} blob client configured with managed identity, container={}, accountName={}",
                    storageOrigin, containerValue, accountName.get());
            return Optional.of(new AzureBlobClientDefault(containerValue, accountName.get(), clientId));
        }

        // Fallback to connection string authentication
        if (connectionString.filter(s -> !s.isBlank()).isPresent()) {
            log.info("StorageRegistry: {} blob client configured with connection string, container={}", storageOrigin, containerValue);
            return Optional.of(new AzureBlobClientDefault(connectionString.get(), containerValue));
        }

        return Optional.empty();
    }
}
