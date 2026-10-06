package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductExpirationResponse;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.mockito.Mockito;

class ProductServiceImplTest {

  private ProductApi productApi;
  private ProductServiceImpl service;

  @BeforeEach
  void setUp() {
    productApi = Mockito.mock(ProductApi.class);
    service = new ProductServiceImpl(productApi);
  }

  @Test
  void getValidProductUsesTheValidEndpointWithoutTenantQuery() {
    ProductResponse expected = new ProductResponse();
    when(productApi.getValidProductById("prod-id", null)).thenReturn(expected);

    assertSame(expected, service.getValidProduct("prod-id"));
    verify(productApi).getValidProductById("prod-id", null);
  }

  @Test
  void getProductExpirationDaysUsesTheEndpointValue() {
    ProductExpirationResponse response = new ProductExpirationResponse();
    response.setExpirationDays(45);
    when(productApi.getProductExpirationDays("prod-id", null)).thenReturn(response);

    assertEquals(45, service.getProductExpirationDays("prod-id"));
  }

  @Test
  void getProductExpirationDaysDefaultsWhenResponseIsNull() {
    when(productApi.getProductExpirationDays("prod-id", null)).thenReturn(null);

    assertEquals(30, service.getProductExpirationDays("prod-id"));
  }

  @Test
  void mapsNotFoundToDomainException() {
    when(productApi.getProductById("missing", null))
        .thenThrow(new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build()));

    ResourceNotFoundException exception = assertThrows(
        ResourceNotFoundException.class, () -> service.getProduct("missing"));
    assertEquals("Product not found with id: missing", exception.getMessage());
  }

  @Test
  void propagatesNonNotFoundFailures() {
    WebApplicationException exception = new WebApplicationException(
        Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
    when(productApi.getProductById("prod-id", null)).thenThrow(exception);

    assertSame(exception, assertThrows(WebApplicationException.class, () -> service.getProduct("prod-id")));
  }

  @Test
  void listPreservesFiltersAndOmitsTenantQuery() {
    List<ProductResponse> expected = List.of(new ProductResponse());
    when(productApi.getProducts(false, false, null)).thenReturn(expected);

    assertSame(expected, service.getProducts(false, false));
    verify(productApi).getProducts(false, false, null);
  }
}

