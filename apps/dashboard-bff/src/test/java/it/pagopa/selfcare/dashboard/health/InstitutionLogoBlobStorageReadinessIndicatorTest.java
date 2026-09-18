package it.pagopa.selfcare.dashboard.health;

import it.pagopa.selfcare.dashboard.client.azure_storage.AzureBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class InstitutionLogoBlobStorageReadinessIndicatorTest {

    private AzureBlobClient blobClient;
    private InstitutionLogoBlobStorageReadinessIndicator indicator;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        indicator = new InstitutionLogoBlobStorageReadinessIndicator(
                blobClient, "$web", "web-account");
    }

    @Test
    void returnsUpWhenLogoContainerIsReachable() {
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("blob-storage", health.getDetails().get("component"));
        assertEquals("web-account", health.getDetails().get("account"));
        assertEquals("$web", health.getDetails().get("container"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void returnsDownWhenLogoContainerIsNotReachable() {
        doThrow(new RuntimeException("connection refused")).when(blobClient).probeContainer();

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("RuntimeException: connection refused", health.getDetails().get("error"));
    }
}
