package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface OnboardedProductMapper {

    OnboardedProductResponse toResponse(OnboardedProduct onboardedProduct);

    List<OnboardedProductResponse> toList(List<OnboardedProduct> onboardedProducts);

    @Mapping(target = "status",  expression = "java(it.pagopa.selfcare.user.model.constants.OnboardedProductState.ACTIVE)")
    @Mapping(target = "env",  expression = "java(it.pagopa.selfcare.onboarding.common.Env.ROOT)")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "productRole", source = "productRole")
    OnboardedProduct toNewOnboardedProduct(CreateUserDto.Product product, String productRole);

    @Mapping(target = "status",  expression = "java(it.pagopa.selfcare.user.model.constants.OnboardedProductState.ACTIVE)")
    @Mapping(target = "env",  expression = "java(it.pagopa.selfcare.onboarding.common.Env.ROOT)")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "productRole", source = "productRole")
    OnboardedProduct toNewOnboardedProduct(AddUserRoleDto.Product product, String productRole);
}
