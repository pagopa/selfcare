package it.pagopa.selfcare.document.config;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class StorageRegistryTest {

    @Inject
    StorageRegistry storageRegistry;

    @Test
    void clientFor_whenOriginIsSystem_shouldReturnSystemClient() {
        AzureBlobClient client = storageRegistry.clientFor(StorageOrigin.SYSTEM);

        assertNotNull(client);
    }

    @Test
    void clientFor_whenOriginIsNull_shouldFallbackToSystemClient() {
        AzureBlobClient clientNull = storageRegistry.clientFor(null);
        AzureBlobClient clientSystem = storageRegistry.clientFor(StorageOrigin.SYSTEM);

        assertNotNull(clientNull);
        assertSame(clientSystem, clientNull);
    }

    @Test
    void clientFor_whenOriginIsUser_shouldReturnDedicatedUserClient() {
        AzureBlobClient userClient = storageRegistry.clientFor(StorageOrigin.USER);
        AzureBlobClient systemClient = storageRegistry.clientFor(StorageOrigin.SYSTEM);

        assertNotNull(userClient);
        assertNotNull(systemClient);
    }

    @Test
    void clientFor_nullAndSystem_shouldBothReturnSameClient() {
        AzureBlobClient fromSystem = storageRegistry.clientFor(StorageOrigin.SYSTEM);
        AzureBlobClient fromNull = storageRegistry.clientFor(null);

        assertSame(fromSystem, fromNull);
    }
}
