package it.pagopa.selfcare.mscore.connector.azure_storage.health;

import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.mscore.connector.azure_storage.AzureBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class FileBlobStorageReadinessIndicatorTest {

    private AzureBlobClient blobClient;
    private TenantRegistry tenantRegistry;
    private FileBlobStorageReadinessIndicator indicator;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        tenantRegistry = mock(TenantRegistry.class);
        when(tenantRegistry.supportedTenantIds())
                .thenReturn(new LinkedHashSet<>(List.of("AR", "PNPG")));
        indicator = new FileBlobStorageReadinessIndicator(blobClient, tenantRegistry);
    }

    @Test
    void returnsUpWhenContainerIsReachable() {
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("blob-storage", health.getDetails().get("component"));
        assertEquals("tenant-specific", health.getDetails().get("account"));
        assertEquals("tenant-specific", health.getDetails().get("container"));
        assertEquals(Set.of("AR", "PNPG"), health.getDetails().get("tenants"));
        assertEquals("mail-templates", health.getDetails().get("storageKey"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
        verify(blobClient).probeContainer("AR");
        verify(blobClient).probeContainer("PNPG");
    }

    @Test
    void returnsDownWhenContainerIsNotReachable() {
        doThrow(new RuntimeException("connection refused"))
                .when(blobClient).probeContainer("PNPG");

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(
                "IllegalStateException: Blob storage probe failed for tenant PNPG and key mail-templates",
                health.getDetails().get("error"));
        verify(blobClient).probeContainer("AR");
        verify(blobClient).probeContainer("PNPG");
    }
}
