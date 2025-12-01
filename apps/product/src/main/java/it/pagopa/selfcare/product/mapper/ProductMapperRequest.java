package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.Product;
import org.mapstruct.*;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapperRequest {

    Product toProduct(ProductCreateRequest productCreateRequest);

    @BeanMapping(ignoreByDefault = false)
    Product cloneObject(@MappingTarget Product target, Product source);

    @BeanMapping(nullValuePropertyMappingStrategy =  NullValuePropertyMappingStrategy.IGNORE)
    Product toPatch(ProductPatchRequest source, @MappingTarget Product target);
}
