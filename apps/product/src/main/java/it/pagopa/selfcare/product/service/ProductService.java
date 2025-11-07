package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.response.ProductResponse;

public interface ProductService {
    Uni<String> ping();

    Uni<String> createProduct(ProductCreateRequest product);

    Uni<ProductResponse> getProductById(String id);
}
