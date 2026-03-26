package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", implementationName = "RestProductMapperImpl")
public interface ProductMapper {

    OriginResult toOriginResult(ProductOriginResponse productOriginResponse);
}
