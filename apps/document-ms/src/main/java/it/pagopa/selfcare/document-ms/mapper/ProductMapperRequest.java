package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import org.mapstruct.*;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapperRequest {

  Product toProduct(ProductCreateRequest productCreateRequest);

  @BeanMapping(ignoreByDefault = false)
  Product cloneObject(@MappingTarget Product target, Product source);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  Product toPatch(ProductPatchRequest source, @MappingTarget Product target);
}
