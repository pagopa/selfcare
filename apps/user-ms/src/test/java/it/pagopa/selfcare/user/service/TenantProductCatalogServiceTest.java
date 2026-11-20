package it.pagopa.selfcare.user.service;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import jakarta.enterprise.inject.Vetoed;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

class TenantProductCatalogServiceTest {
  @Vetoed
  static class TestCatalog extends TenantProductCatalogService {
    Map<String, AzureBlobClientDefault> blobs;
    @Override protected AzureBlobClientDefault createClient(String tenant) { return blobs.get(tenant); }
  }

  @Test void sameProductIdUsesIndependentClientsAndCaches() {
    TestCatalog catalog = new TestCatalog();
    catalog.registry = mock(TenantRegistry.class);
    catalog.context = mock(TenantContext.class);
    catalog.filePath = "products.json";
    when(catalog.registry.normalizeTenantId(anyString())).thenAnswer(call -> call.getArgument(0));
    for (String tenant : new String[]{"AR", "PNPG"}) {
      when(catalog.registry.storage(tenant, "products")).thenReturn(
          new TenantDefinition.StorageDefinition("local", "products-" + tenant, "", null));
    }
    AzureBlobClientDefault ar = blob("AR");
    AzureBlobClientDefault pnpg = blob("PNPG");
    catalog.blobs = Map.of("AR", ar, "PNPG", pnpg);
    when(catalog.context.requiredTenantId()).thenReturn("AR");
    assertEquals("AR", catalog.getProduct("shared").getTitle());
    when(catalog.context.requiredTenantId()).thenReturn("PNPG");
    assertEquals("PNPG", catalog.getProduct("shared").getTitle());
    when(catalog.context.requiredTenantId()).thenReturn("AR");
    assertEquals("AR", catalog.getProduct("shared").getTitle());
    for (String tenant : new String[]{"AR", "PNPG"}) {
      when(catalog.context.requiredTenantId()).thenReturn(tenant);
      assertEquals(tenant, catalog.getProducts(true, true).get(0).getTitle());
      assertEquals(tenant, catalog.productsForTenant(tenant).get(0).getTitle());
      assertEquals(tenant, catalog.getProductRaw("shared").getTitle());
      assertEquals(tenant, catalog.getProductIsValid("shared").getTitle());
      assertEquals("admin", catalog.validateProductRole("shared", "admin", PartyRole.MANAGER).getCode());
      assertDoesNotThrow(() -> catalog.validateRoleMappings(catalog.getProduct("shared").getRoleMappings()));
      assertTrue(catalog.verifyAllowedByInstitutionTaxCode("shared", tenant + "-tax"));
      assertFalse(catalog.verifyAllowedByInstitutionTaxCode("shared", "other-tax"));
      assertEquals(42, catalog.getProductExpirationDate("shared"));
      assertTrue(catalog.isProductEnabled("shared"));
      assertThrows(ProductNotFoundException.class, () -> catalog.getProduct("missing"));
    }
    verify(ar, times(1)).getFileAsText("products.json");
    verify(pnpg, times(1)).getFileAsText("products.json");
  }

  @Test void productPathsRespectEachTenantsPrefix() {
    TenantProductCatalogService catalog = new TenantProductCatalogService();
    catalog.registry = mock(TenantRegistry.class);
    catalog.filePath = "products.json";
    for (String prefix : new String[]{"", " ", "catalog/", "catalog///"}) {
      when(catalog.registry.storage("AR", "products")).thenReturn(
          new TenantDefinition.StorageDefinition("local", "products-ar", prefix, null));
      assertEquals(prefix.isBlank() ? "products.json" : "catalog/products.json", catalog.productPath("AR"));
    }
    when(catalog.registry.storage("PNPG", "products")).thenReturn(
        new TenantDefinition.StorageDefinition("local", "products-pnpg", null, null));
    assertEquals("products.json", catalog.productPath("PNPG"));
  }

  @Test void buildsAndCachesClientsFromTheTenantsAuthenticationSettings() {
    TenantProductCatalogService catalog = new TenantProductCatalogService();
    catalog.registry = mock(TenantRegistry.class);
    when(catalog.registry.normalizeTenantId(anyString())).thenAnswer(call -> call.getArgument(0));
    when(catalog.registry.storage("AR", "products")).thenReturn(
        new TenantDefinition.StorageDefinition("local", "products-ar", "",
            new TenantDefinition.StorageAuthentication(StorageAuthenticationType.CONNECTION_STRING, null, "AR_STORAGE")));
    when(catalog.registry.storageConnectionString("AR", "products")).thenReturn(Optional.of(
        "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=dGVzdA==;BlobEndpoint=http://localhost:10000/devstoreaccount1;"));
    when(catalog.registry.storage("PNPG", "products")).thenReturn(
        new TenantDefinition.StorageDefinition("testaccount", "products-pnpg", "",
            new TenantDefinition.StorageAuthentication(StorageAuthenticationType.MANAGED_IDENTITY, "PNPG_IDENTITY", null)));
    when(catalog.registry.storageManagedIdentityClientId("PNPG", "products"))
        .thenReturn(Optional.of("00000000-0000-0000-0000-000000000000"));

    var ar = catalog.blobClient("AR");
    var pnpg = catalog.blobClient("PNPG");
    assertNotSame(ar, pnpg);
    assertSame(ar, catalog.blobClient("AR"));
    assertSame(pnpg, catalog.blobClient("PNPG"));
    verify(catalog.registry, times(1)).storageConnectionString("AR", "products");
    verify(catalog.registry, times(1)).storageManagedIdentityClientId("PNPG", "products");
  }

  private AzureBlobClientDefault blob(String title) {
    AzureBlobClientDefault client = mock(AzureBlobClientDefault.class);
    BlobProperties properties = new BlobProperties(
        null, OffsetDateTime.parse("2025-01-01T00:00:00Z"), null, 0,
        null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, Map.of(), null);
    when(client.getProperties("products.json")).thenReturn(properties);
    when(client.getFileAsText("products.json")).thenReturn(
        "[{\"id\":\"shared\",\"title\":\"" + title + "\",\"status\":\"ACTIVE\",\"enabled\":true,"
            + "\"expirationDate\":42,\"allowedInstitutionTaxCode\":[\"" + title + "-tax\"],"
            + "\"roleMappings\":{\"MANAGER\":{\"roles\":[{\"code\":\"admin\"}]}}}]");
    return client;
  }
}
