package it.pagopa.selfcare.mscore.core.health;

import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ProductBlobStorageReadinessIndicatorTest {

    private AzureBlobClientDefault blobClient;
    private ProductBlobStorageReadinessIndicator indicator;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClientDefault.class);
        CoreConfig.BlobStorage blobStorage = new CoreConfig.BlobStorage();
        blobStorage.setAccountNameProduct("product-account");
        blobStorage.setContainerProduct("products");
        blobStorage.setFilepathProduct("products.json");
        CoreConfig coreConfig = new CoreConfig();
        coreConfig.setBlobStorage(blobStorage);
        indicator = new ProductBlobStorageReadinessIndicator(blobClient, coreConfig);
    }

    @Test
    void returnsUpWhenProductCatalogIsReadable() {
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("blob-storage", health.getDetails().get("component"));
        assertEquals("product-account", health.getDetails().get("account"));
        assertEquals("products", health.getDetails().get("container"));
        assertEquals("products.json", health.getDetails().get("probeTarget"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void returnsDownWhenProductCatalogIsNotReadable() {
        doThrow(new RuntimeException("Status code 403"))
                .when(blobClient).getProperties("products.json");

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(
                "RuntimeException: Status code 403",
                health.getDetails().get("error"));
    }
}
