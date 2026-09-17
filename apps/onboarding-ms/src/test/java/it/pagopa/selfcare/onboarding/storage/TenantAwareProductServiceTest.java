package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TenantAwareProductServiceTest {

    @Test
    void getProduct_usesTenantSpecificBlobClient() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        AzureBlobClient pnpgClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        when(blobClientProvider.clientFor("PNPG", StorageKeys.PRODUCTS)).thenReturn(pnpgClient);
        stubProductJson(arClient, "prod-ar", "AR Product");
        stubProductJson(pnpgClient, "prod-pnpg", "PNPG Product");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        when(tenantContext.requiredTenantId()).thenReturn("AR");
        Product arProduct = service.getProduct("prod-ar");
        when(tenantContext.requiredTenantId()).thenReturn("PNPG");
        Product pnpgProduct = service.getProduct("prod-pnpg");

        assertEquals("AR Product", arProduct.getTitle());
        assertEquals("PNPG Product", pnpgProduct.getTitle());
        assertNotSame(arProduct, pnpgProduct);
    }

    private static void stubProductJson(AzureBlobClient client, String productId, String title) {
        BlobProperties properties = new BlobProperties(
                OffsetDateTime.now(), OffsetDateTime.now(), null, 0L, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, Map.of(), null);
        when(client.getProperties("products.json")).thenReturn(properties);
        when(client.getFileAsText("products.json"))
                .thenReturn("[{\"id\":\"" + productId + "\",\"title\":\"" + title + "\"}]");
    }
}
