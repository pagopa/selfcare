package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface OnboardedProductMapper {

    OnboardedProductResponse toResponse(OnboardedProduct onboardedProduct);

    List<OnboardedProductResponse> toList(List<OnboardedProduct> onboardedProducts);

    @Mapping(target = "status",  expression = "java(it.pagopa.selfcare.user.constant.OnboardedProductState.ACTIVE)")
    @Mapping(target = "env",  expression = "java(it.pagopa.selfcare.onboarding.common.Env.ROOT)")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    OnboardedProduct toNewOnboardedProduct(CreateUserDto.Product product);
}
