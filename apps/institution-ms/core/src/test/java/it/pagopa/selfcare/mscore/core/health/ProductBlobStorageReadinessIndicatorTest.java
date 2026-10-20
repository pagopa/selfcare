package it.pagopa.selfcare.mscore.core.health;

import com.azure.storage.blob.BlobClient;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClient;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
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

class ProductBlobStorageReadinessIndicatorTest {

    private TenantBlobClientProvider blobClientProvider;
    private TenantRegistry tenantRegistry;
    private TenantBlobClient arStorage;
    private TenantBlobClient pnpgStorage;
    private BlobClient arBlobClient;
    private BlobClient pnpgBlobClient;
    private ProductBlobStorageReadinessIndicator indicator;

    @BeforeEach
    void setUp() {
        blobClientProvider = mock(TenantBlobClientProvider.class);
        tenantRegistry = mock(TenantRegistry.class);
        arStorage = mock(TenantBlobClient.class);
        pnpgStorage = mock(TenantBlobClient.class);
        arBlobClient = mock(BlobClient.class);
        pnpgBlobClient = mock(BlobClient.class);
        when(tenantRegistry.supportedTenantIds())
                .thenReturn(new LinkedHashSet<>(List.of("AR", "PNPG")));
        when(blobClientProvider.clientFor("AR", "products")).thenReturn(arStorage);
        when(blobClientProvider.clientFor("PNPG", "products")).thenReturn(pnpgStorage);
        when(arStorage.blob("products.json")).thenReturn(arBlobClient);
        when(pnpgStorage.blob("products.json")).thenReturn(pnpgBlobClient);
        indicator = new ProductBlobStorageReadinessIndicator(
                blobClientProvider, tenantRegistry, "products.json");
    }

    @Test
    void returnsUpWhenProductCatalogIsReadable() {
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("blob-storage", health.getDetails().get("component"));
        assertEquals("tenant-specific", health.getDetails().get("account"));
        assertEquals("tenant-specific", health.getDetails().get("container"));
        assertEquals("products.json", health.getDetails().get("probeTarget"));
        assertEquals(Set.of("AR", "PNPG"), health.getDetails().get("tenants"));
        assertEquals("products", health.getDetails().get("storageKey"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
        org.mockito.Mockito.verify(arStorage).blob("products.json");
        org.mockito.Mockito.verify(pnpgStorage).blob("products.json");
    }

    @Test
    void returnsDownWhenProductCatalogIsNotReadable() {
        doThrow(new RuntimeException("Status code 403")).when(pnpgBlobClient).getProperties();

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(
                "IllegalStateException: Blob storage probe failed for tenant PNPG and key products",
                health.getDetails().get("error"));
    }
}
