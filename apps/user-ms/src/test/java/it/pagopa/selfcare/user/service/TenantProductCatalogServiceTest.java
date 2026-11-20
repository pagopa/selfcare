package it.pagopa.selfcare.user.service;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.inject.Vetoed;
import java.time.OffsetDateTime;
import java.util.Map;
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
    verify(ar, times(1)).getFileAsText("products.json");
    verify(pnpg, times(1)).getFileAsText("products.json");
  }

  private AzureBlobClientDefault blob(String title) {
    AzureBlobClientDefault client = mock(AzureBlobClientDefault.class);
    BlobProperties properties = new BlobProperties(
        null, OffsetDateTime.parse("2025-01-01T00:00:00Z"), null, 0,
        null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, Map.of(), null);
    when(client.getProperties("products.json")).thenReturn(properties);
    when(client.getFileAsText("products.json")).thenReturn(
        "[{\"id\":\"shared\",\"title\":\"" + title + "\",\"status\":\"ACTIVE\"}]");
    return client;
  }
}
