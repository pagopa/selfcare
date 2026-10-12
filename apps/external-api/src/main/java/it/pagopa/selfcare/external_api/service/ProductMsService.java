package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;

import java.util.List;

public interface ProductMsService {

    List<ProductResponse> getProducts(boolean rootOnly, boolean valid);

    ProductResponse getProductRaw(String productId);
}

