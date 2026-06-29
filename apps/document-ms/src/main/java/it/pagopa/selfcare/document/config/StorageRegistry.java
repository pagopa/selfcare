package it.pagopa.selfcare.document.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

@ApplicationScoped
@Slf4j
public class StorageRegistry {

    private final Map<StorageOrigin, AzureBlobClient> clientsByOrigin;

    public StorageRegistry(
            AzureBlobClient systemBlobClient,
            @ConfigProperty(name = "document-ms.blob-storage.connection-string-user") Optional<String> userConnectionString,
            @ConfigProperty(name = "document-ms.blob-storage.container-user") Optional<String> userContainer) {

        Map<StorageOrigin, AzureBlobClient> clients = new EnumMap<>(StorageOrigin.class);
        clients.put(StorageOrigin.SYSTEM, systemBlobClient);
        clients.put(StorageOrigin.USER, buildUserBlobClient(systemBlobClient, userConnectionString, userContainer));

        this.clientsByOrigin = Map.copyOf(clients);
    }

    public AzureBlobClient clientFor(StorageOrigin origin) {
        if (Objects.isNull(origin)) {
            log.debug("StorageRegistry: storageOrigin is null (legacy document), routing to SYSTEM");
            return clientsByOrigin.get(StorageOrigin.SYSTEM);
        }
        AzureBlobClient client = clientsByOrigin.getOrDefault(origin, clientsByOrigin.get(StorageOrigin.SYSTEM));
        log.debug("StorageRegistry: routing storageOrigin={} to client={}", origin, client.getClass().getSimpleName());
        return client;
    }

    private AzureBlobClient buildUserBlobClient(AzureBlobClient systemBlobClient,
                                                Optional<String> connectionString,
                                                Optional<String> container) {
        boolean isConfigured = connectionString.isPresent() && container.isPresent()
                && !connectionString.get().isBlank() && !container.get().isBlank();

        if (isConfigured) {
            log.info("StorageRegistry: USER blob client configured with container={}", container.get());
            return new AzureBlobClientDefault(connectionString.get(), container.get());
        }

        log.info("StorageRegistry: USER blob client not configured, falling back to SYSTEM");
        return systemBlobClient;
    }
}
