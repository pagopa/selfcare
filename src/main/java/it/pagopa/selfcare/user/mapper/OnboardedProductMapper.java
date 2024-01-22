package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface OnboardedProductMapper {

    OnboardedProductResponse toResponse(OnboardedProduct onboardedProduct);
    List<OnboardedProductResponse> toList(List<OnboardedProduct> onboardedProducts);

}
