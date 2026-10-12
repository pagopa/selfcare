package it.pagopa.selfcare.onboarding.service;

import java.util.List;
import org.openapi.quarkus.product_json.model.ProductResponse;

public interface ProductService {

  ProductResponse getProduct(String productId);

  ProductResponse getValidProduct(String productId);

  Integer getProductExpirationDays(String productId);

  List<ProductResponse> getProducts(boolean rootOnly, boolean valid);
}
