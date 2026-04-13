package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.controller.response.OriginResponse;
import it.pagopa.selfcare.onboarding.controller.response.ProductResource;
import it.pagopa.selfcare.product.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ProductMapper {
    ProductResource toResource(Product model);

    OriginResponse toOriginResponse(OriginResult originEntries);
}
