package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi", implementationName = "RestProductMapperImpl")
public interface ProductMapper {

    OriginResult toOriginResult(ProductOriginResponse productOriginResponse);
}
