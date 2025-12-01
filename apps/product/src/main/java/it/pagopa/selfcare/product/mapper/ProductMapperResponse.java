package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductOriginResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;
import it.pagopa.selfcare.product.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapperResponse {

    ProductResponse toProductResponse(Product product);

    ProductBaseResponse toProductBaseResponse(Product product);

    @Mapping(target = "origins", source = "institutionOrigins")
    ProductOriginResponse toProductOriginResponse(Product product);

}
