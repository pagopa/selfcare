package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TenantAwareProductServiceTest {

    private static final String PRODUCT_JSON =
            "[{\"id\":\"prod-io\",\"title\":\"AR Product\",\"status\":\"ACTIVE\",\"roleMappings\":{\"MANAGER\":{\"roles\":[{\"code\":\"admin\",\"label\":\"Admin\"}]}}}]";

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

    @Test
    void delegate_isCachedPerTenant() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-ar", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        service.getProduct("prod-ar");
        service.getProduct("prod-ar");
        service.isProductEnabled("prod-ar");

        verify(blobClientProvider, times(1)).clientFor("AR", StorageKeys.PRODUCTS);
    }

    @Test
    void getProducts_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-io", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        assertEquals(1, service.getProducts(false, false).size());
    }

    @Test
    void validateRoleMappings_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-io", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");
        ProductRole role = new ProductRole();
        role.setCode("admin");
        ProductRoleInfo roleInfo = new ProductRoleInfo();
        roleInfo.setRoles(java.util.List.of(role));
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, roleInfo);

        service.validateRoleMappings(roleMappings);
    }

    @Test
    void validateRoleMappings_throwsWhenEmpty() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-io", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> service.validateRoleMappings(Map.of()));
    }

    @Test
    void getProductRaw_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-io", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        assertEquals("AR Product", service.getProductRaw("prod-io").getTitle());
    }

    @Test
    void getProductIsValid_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-io", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        assertEquals("AR Product", service.getProductIsValid("prod-io").getTitle());
    }

    @Test
    void validateProductRole_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJsonBody(arClient, PRODUCT_JSON);
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");
        ProductRole role = service.validateProductRole("prod-io", "admin", PartyRole.MANAGER);

        assertEquals("admin", role.getCode());
    }

    @Test
    void verifyAllowedByInstitutionTaxCode_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJsonBody(arClient,
                "[{\"id\":\"prod-io\",\"title\":\"AR Product\",\"status\":\"ACTIVE\",\"allowedInstitutionTaxCode\":[\"ANY-TAX-CODE\"]}]");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        assertTrue(service.verifyAllowedByInstitutionTaxCode("prod-io", "any-tax-code"));
        assertFalse(service.verifyAllowedByInstitutionTaxCode("prod-io", "other-tax-code"));
    }

    @Test
    void getProductExpirationDate_returnsDefaultWhenNotConfigured() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJson(arClient, "prod-io", "AR Product");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        assertEquals(30, service.getProductExpirationDate("prod-io"));
    }

    @Test
    void isProductEnabled_delegatesToTenantSpecificCatalogue() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantBlobClientProvider blobClientProvider = mock(TenantBlobClientProvider.class);
        AzureBlobClient arClient = mock(AzureBlobClient.class);
        when(blobClientProvider.clientFor("AR", StorageKeys.PRODUCTS)).thenReturn(arClient);
        stubProductJsonBody(arClient,
                "[{\"id\":\"prod-io\",\"title\":\"AR Product\",\"status\":\"ACTIVE\",\"enabled\":false},"
                        + "{\"id\":\"prod-pnpg\",\"title\":\"PNPG Product\",\"status\":\"ACTIVE\",\"enabled\":true}]");
        when(tenantContext.requiredTenantId()).thenReturn("AR");

        TenantAwareProductService service = new TenantAwareProductService(
                tenantContext, blobClientProvider, "products.json");

        assertFalse(service.isProductEnabled("prod-io"));
        assertTrue(service.isProductEnabled("prod-pnpg"));
    }

    private static void stubProductJson(AzureBlobClient client, String productId, String title) {
        stubProductJsonBody(client,
                "[{\"id\":\"" + productId + "\",\"title\":\"" + title + "\",\"status\":\"ACTIVE\"}]");
    }

    private static void stubProductJsonBody(AzureBlobClient client, String body) {
        BlobProperties properties = new BlobProperties(
                OffsetDateTime.now(), OffsetDateTime.now(), null, 0L, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, Map.of(), null);
        when(client.getProperties("products.json")).thenReturn(properties);
        when(client.getFileAsText("products.json")).thenReturn(body);
    }
}
