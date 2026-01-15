package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;

public interface ProductService {
    Uni<String> ping();

    Uni<ProductBaseResponse> createProduct(ProductCreateRequest product, String createdBy);

    Uni<ProductResponse> getProductById(String productId);

    Uni<ProductBaseResponse> deleteProductById(String productId);

    Uni<ProductResponse> patchProductById(String productId, String createdBy, ProductPatchRequest productPatchRequest);

    Uni<ProductOriginResponse> getProductOriginsById(String productId);
}
