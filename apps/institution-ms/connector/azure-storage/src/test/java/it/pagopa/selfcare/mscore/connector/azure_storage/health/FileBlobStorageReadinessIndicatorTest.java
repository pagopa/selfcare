package it.pagopa.selfcare.mscore.connector.azure_storage.health;

import it.pagopa.selfcare.mscore.config.AzureStorageConfig;
import it.pagopa.selfcare.mscore.connector.azure_storage.AzureBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class FileBlobStorageReadinessIndicatorTest {

    private AzureBlobClient blobClient;
    private FileBlobStorageReadinessIndicator indicator;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        AzureStorageConfig config = new AzureStorageConfig();
        config.setAccountName("storage-account");
        config.setContainer("resources");
        indicator = new FileBlobStorageReadinessIndicator(blobClient, config);
    }

    @Test
    void returnsUpWhenContainerIsReachable() {
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("blob-storage", health.getDetails().get("component"));
        assertEquals("storage-account", health.getDetails().get("account"));
        assertEquals("resources", health.getDetails().get("container"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void returnsDownWhenContainerIsNotReachable() {
        doThrow(new RuntimeException("connection refused"))
                .when(blobClient).probeContainer();

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(
                "RuntimeException: connection refused",
                health.getDetails().get("error"));
    }
}
