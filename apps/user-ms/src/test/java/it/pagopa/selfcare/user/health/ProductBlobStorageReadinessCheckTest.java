package it.pagopa.selfcare.user.health;

import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.user.service.TenantProductCatalogService;
import java.time.Duration;
import java.util.Set;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductBlobStorageReadinessCheckTest {
  ProductBlobStorageReadinessCheck check;
  AzureBlobClientDefault ar;
  AzureBlobClientDefault pnpg;

  @BeforeEach void setup() {
    check = new ProductBlobStorageReadinessCheck();
    check.registry = mock(TenantRegistry.class, RETURNS_DEEP_STUBS);
    check.catalogs = mock(TenantProductCatalogService.class);
    when(check.registry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
    ar = client("AR");
    pnpg = client("PNPG");
  }

  private AzureBlobClientDefault client(String tenant) {
    when(check.registry.storage(tenant, "products")).thenReturn(
        new it.pagopa.selfcare.tenant.TenantDefinition.StorageDefinition("local", "products-" + tenant, "", null));
    when(check.catalogs.productPath(tenant)).thenReturn("products.json");
    when(check.catalogs.hasSnapshot(tenant)).thenReturn(true);
    AzureBlobClientDefault client = mock(AzureBlobClientDefault.class);
    when(check.catalogs.blobClient(tenant)).thenReturn(client);
    return client;
  }

  @Test void probesEveryTenantWithoutRequestContext() {
    var response = check.call().await().atMost(Duration.ofSeconds(5));
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("products-PNPG", response.getData().orElseThrow().get("PNPG.container"));
    verify(ar).getProperties("products.json");
    verify(pnpg).getProperties("products.json");
  }

  @Test void reportsFailingTenantAndAttemptsBothContainers() {
    when(pnpg.getProperties("products.json")).thenThrow(new IllegalStateException("403"));
    var response = check.call().await().atMost(Duration.ofSeconds(5));
    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    assertTrue(response.getData().orElseThrow().get("error").toString().contains("[PNPG]"));
    verify(ar).getProperties("products.json");
  }

  @Test void isDownUntilEveryTenantCatalogueIsLoaded() {
    when(check.catalogs.hasSnapshot("PNPG")).thenReturn(false);
    var response = check.call().await().atMost(Duration.ofSeconds(5));
    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    verifyNoInteractions(pnpg);
    verify(ar).getProperties("products.json");
  }
}
