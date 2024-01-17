package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface OnboardedProductMapper {

    OnboardedProductResponse toResponse(OnboardedProduct onboardedProduct);

}
