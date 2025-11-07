package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.controller.response.ProductResponse;
import it.pagopa.selfcare.product.model.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface ProductMapperResponse {

    ProductResponse toProductResponse(Product product);

}
