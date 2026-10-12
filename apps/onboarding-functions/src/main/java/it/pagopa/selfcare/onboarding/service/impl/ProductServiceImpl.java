package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Objects;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProductServiceImpl implements ProductService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);
  private static final int DEFAULT_EXPIRATION_DAYS = 30;

  private final ProductApi productApi;

  public ProductServiceImpl(@RestClient ProductApi productApi) {
    this.productApi = productApi;
  }

  @Override
  public ProductResponse getProduct(String productId) {
    return invoke(productId, () -> productApi.getProductById(productId, null));
  }

  @Override
  public ProductResponse getValidProduct(String productId) {
    return invoke(productId, () -> productApi.getValidProductById(productId, null));
  }

  @Override
  public Integer getProductExpirationDays(String productId) {
    return invoke(productId, () -> {
      var response = productApi.getProductExpirationDays(productId, null);
      return response == null || response.getExpirationDays() == null
          ? DEFAULT_EXPIRATION_DAYS
          : response.getExpirationDays();
    });
  }

  @Override
  public List<ProductResponse> getProducts(boolean rootOnly, boolean valid) {
    try {
      return productApi.getProducts(rootOnly, valid, null);
    } catch (WebApplicationException exception) {
      throw mapNotFound(exception, "<list>");
    }
  }

  private <T> T invoke(String productId, Request<T> request) {
    try {
      LOGGER.debug("Calling product-ms for productId={}", Encode.forJava(String.valueOf(productId)));
      return request.execute();
    } catch (WebApplicationException exception) {
      throw mapNotFound(exception, productId);
    }
  }

  private RuntimeException mapNotFound(WebApplicationException exception, String productId) {
    if (Objects.nonNull(exception.getResponse()) && exception.getResponse().getStatus() == 404) {
      return new ResourceNotFoundException("Product not found with id: " + productId);
    }
    throw exception;
  }

  @FunctionalInterface
  private interface Request<T> {
    T execute();
  }
}
