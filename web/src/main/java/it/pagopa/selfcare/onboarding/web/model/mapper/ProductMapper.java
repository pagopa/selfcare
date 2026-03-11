package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.web.model.OriginResponse;
import it.pagopa.selfcare.onboarding.web.model.ProductResource;
import it.pagopa.selfcare.product.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResource toResource(Product model);

    OriginResponse toOriginResponse(OriginResult originEntries);
}
