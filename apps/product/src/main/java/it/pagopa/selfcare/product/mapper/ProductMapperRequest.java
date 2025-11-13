package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface ProductMapperRequest {

    Product toProduct(ProductCreateRequest productCreateRequest);

    @BeanMapping(ignoreByDefault = false)
    Product cloneObject(@MappingTarget Product target, Product source);
}
