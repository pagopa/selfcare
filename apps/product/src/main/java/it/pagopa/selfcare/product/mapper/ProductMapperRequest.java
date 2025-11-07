package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface ProductMapperRequest {

    Product toProduct(ProductCreateRequest productCreateRequest);

}
