package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.Product;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface ProductMapper {
  Product toEntity(it.pagopa.selfcare.product.entity.Product product);
  it.pagopa.selfcare.product.entity.Product fromModel(Product product);
}
