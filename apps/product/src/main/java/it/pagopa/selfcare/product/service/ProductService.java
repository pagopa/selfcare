package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;

public interface ProductService {
    Uni<String> ping();

    Uni<ProductBaseResponse> createProduct(ProductCreateRequest product);

    Uni<ProductResponse> getProductById(String id);
}
