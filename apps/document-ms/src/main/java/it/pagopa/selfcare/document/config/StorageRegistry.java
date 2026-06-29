package it.pagopa.selfcare.document.config;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@Slf4j
public class StorageRegistry {

    private final Map<StorageOrigin, AzureBlobClient> registry;

    public StorageRegistry(
            AzureBlobClient systemClient,
            @ConfigProperty(name = "document-ms.blob-storage.connection-string-user", defaultValue = "") String userConnectionString,
            @ConfigProperty(name = "document-ms.blob-storage.container-user", defaultValue = "") String userContainer) {

        Map<StorageOrigin, AzureBlobClient> map = new EnumMap<>(StorageOrigin.class);
        map.put(StorageOrigin.SYSTEM, systemClient);

        if (!userConnectionString.isBlank() && !userContainer.isBlank()) {
            log.info("StorageRegistry: USER storage configured with container={}", userContainer);
            map.put(StorageOrigin.USER, new AzureBlobClientDefault(userConnectionString, userContainer));
        } else {
            log.info("StorageRegistry: USER storage not configured, falling back to SYSTEM storage");
            map.put(StorageOrigin.USER, systemClient);
        }

        this.registry = Map.copyOf(map);
    }

    public AzureBlobClient clientFor(StorageOrigin origin) {
        if (Objects.isNull(origin)) {
            log.debug("StorageRegistry: storageOrigin is null (legacy document), routing to SYSTEM");
            return registry.get(StorageOrigin.SYSTEM);
        }
        AzureBlobClient client = registry.getOrDefault(origin, registry.get(StorageOrigin.SYSTEM));
        log.debug("StorageRegistry: routing storageOrigin={} to client={}", origin, client.getClass().getSimpleName());
        return client;
    }
}
