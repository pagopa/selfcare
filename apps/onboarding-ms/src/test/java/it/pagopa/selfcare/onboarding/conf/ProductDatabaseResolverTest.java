package it.pagopa.selfcare.onboarding.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.UnresolvableProductDatabaseException;
import it.pagopa.selfcare.product.entity.DataIsolationConfig;
import it.pagopa.selfcare.product.entity.DatabaseIsolationModel;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Verifies the product-to-database mapping, and in particular that it never silently falls back to
 * the shared database for a product that was configured out of it.
 */
class ProductDatabaseResolverTest {

  private static final String SHARED = "selcOnboarding";

  @Mock ProductService productService;

  ProductDatabaseResolver resolver;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resolver = new ProductDatabaseResolver(productService, SHARED);
  }

  private Product product(DatabaseIsolationModel model, String databaseName) {
    Product product = new Product();
    product.setId("prod-test");
    if (model != null) {
      DataIsolationConfig config = new DataIsolationConfig();
      config.setDatabase(model);
      config.setDatabaseName(databaseName);
      product.setDataIsolation(config);
    }
    return product;
  }

  @Test
  void blankProductResolvesToShared() {
    assertEquals(SHARED, resolver.resolveDatabase(null));
    assertEquals(SHARED, resolver.resolveDatabase(""));
    assertEquals(SHARED, resolver.resolveDatabase("  "));
  }

  @Test
  void productWithoutIsolationBlockResolvesToShared() {
    when(productService.getProductRaw(anyString())).thenReturn(product(null, null));

    assertEquals(SHARED, resolver.resolveDatabase("prod-test"));
  }

  @Test
  void sharedProductResolvesToShared() {
    when(productService.getProductRaw(anyString()))
        .thenReturn(product(DatabaseIsolationModel.SHARED, null));

    assertEquals(SHARED, resolver.resolveDatabase("prod-test"));
  }

  @Test
  void dedicatedProductResolvesToItsOwnDatabase() {
    when(productService.getProductRaw(anyString()))
        .thenReturn(product(DatabaseIsolationModel.DEDICATED, "selcOnboardingDedicated"));

    assertEquals("selcOnboardingDedicated", resolver.resolveDatabase("prod-test"));
  }

  @Test
  void dedicatedProductWithoutDatabaseNameFailsClosed() {
    when(productService.getProductRaw(anyString()))
        .thenReturn(product(DatabaseIsolationModel.DEDICATED, "  "));

    assertThrows(
        UnresolvableProductDatabaseException.class, () -> resolver.resolveDatabase("prod-test"));
  }

  @Test
  void unknownProductFailsClosedInsteadOfDefaultingToShared() {
    when(productService.getProductRaw(anyString())).thenReturn(null);

    assertThrows(
        UnresolvableProductDatabaseException.class, () -> resolver.resolveDatabase("prod-unknown"));
  }

  @Test
  void productLookupFailureFailsClosed() {
    when(productService.getProductRaw(anyString()))
        .thenThrow(new IllegalStateException("blob unavailable"));

    assertThrows(
        UnresolvableProductDatabaseException.class, () -> resolver.resolveDatabase("prod-test"));
  }
}
