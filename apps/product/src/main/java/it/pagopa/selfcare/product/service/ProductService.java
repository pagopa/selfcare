package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.request.ProductPatchRequest;
import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductOriginResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;

public interface ProductService {
    Uni<String> ping();

    Uni<ProductBaseResponse> createProduct(ProductCreateRequest product);

    Uni<ProductResponse> getProductById(String productId);

    Uni<ProductBaseResponse> deleteProductById(String productId);

    Uni<ProductBaseResponse> patchProductById(String productId, ProductPatchRequest productPatchRequest);

    Uni<ProductOriginResponse> getProductOriginsById(String productId);
}
