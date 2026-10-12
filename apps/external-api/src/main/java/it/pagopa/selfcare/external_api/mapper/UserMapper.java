package it.pagopa.selfcare.external_api.mapper;


import it.pagopa.selfcare.external_api.model.user.User;
import it.pagopa.selfcare.external_api.model.user.UserInstitution;
import it.pagopa.selfcare.external_api.service.ProductMsService;
import it.pagopa.selfcare.external_api.utils.ProductMsUtils;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDetailResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.Getter;

import java.util.Optional;

@Mapper(componentModel = "spring")
@Getter
public abstract class UserMapper {

    @Autowired
    ProductMsService productMsService;

    public abstract UserInstitution toUserInstitutionsFromUserInstitutionResponse(UserInstitutionResponse userInstitutionResponse);

    public abstract User toUserFromUserDetailResponse(UserDetailResponse onboardingData);

    @Mapping(target = "productRoleLabel", expression = "java(toProductRoleLabel(onboardedProduct, getProductMsService().getProductRaw(onboardedProduct.getProductId())))")
    public abstract it.pagopa.selfcare.external_api.model.user.OnboardedProductResponse
    onboardedProductResponseToOnboardedProductResponse(OnboardedProductResponse onboardedProduct);

    @Named("toProductRoleLabel")
    protected String toProductRoleLabel(OnboardedProductResponse onboardedProduct, ProductResponse product) {
        BackOfficeRole productRole = null;
        try { productRole = ProductMsUtils.getProductRole(onboardedProduct.getProductRole(), PartyRole.valueOf(onboardedProduct.getRole()), product); }
        catch (IllegalArgumentException ignored) {}

        return Optional.ofNullable(productRole)
                //ProductLabel is used when Product Role description is strict different than Selc Role description
                //for ex. prod-pagopa, this should be removed in the future
                .map(productRoleItem -> Optional
                        .ofNullable(productRoleItem.getProductLabel()).
                        orElse(productRoleItem.getLabel()))
                .orElse("N.A.");
    }
}
