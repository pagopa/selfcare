package it.pagopa.selfcare.onboarding.model.mapper;

import it.pagopa.selfcare.onboarding.client.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.model.OriginResponse;
import it.pagopa.selfcare.onboarding.model.ProductResource;
import it.pagopa.selfcare.product.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ProductMapper {
    ProductResource toResource(Product model);

    OriginResponse toOriginResponse(OriginResult originEntries);
}
