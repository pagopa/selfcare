package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.OnboardedProductDataResponse;
import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.util.UserUtils;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "cdi", uses = UserUtils.class)
public interface OnboardedProductMapper {

    @Named("toResponse")
    OnboardedProductResponse toResponse(OnboardedProduct onboardedProduct);

    @Named("toDataResponse")
    @Mapping(
      target = "excludeRoleFromUserGroups",
      source = "onboardedProduct",
      qualifiedByName = "isExcludeRoleFromUserGroups"
    )
    OnboardedProductDataResponse toDataResponse(OnboardedProduct onboardedProduct);

    @IterableMapping(qualifiedByName = "toResponse")
    List<OnboardedProductResponse> toList(List<OnboardedProduct> onboardedProducts);

    @IterableMapping(qualifiedByName = "toDataResponse")
    List<OnboardedProductDataResponse> toDataList(List<OnboardedProduct> onboardedProducts);

    @Mapping(target = "status",  expression = "java(it.pagopa.selfcare.user.model.constants.OnboardedProductState.ACTIVE)")
    @Mapping(target = "env",  expression = "java(it.pagopa.selfcare.onboarding.common.Env.ROOT)")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "productRole", source = "productRole")
    OnboardedProduct toNewOnboardedProduct(CreateUserDto.Product product, String productRole);

    @Mapping(target = "status",  source = "status")
    @Mapping(target = "env",  expression = "java(it.pagopa.selfcare.onboarding.common.Env.ROOT)")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "productRole", source = "productRole")
    OnboardedProduct toNewOnboardedProduct(AddUserRoleDto.Product product, String productRole, OnboardedProductState status);

}
