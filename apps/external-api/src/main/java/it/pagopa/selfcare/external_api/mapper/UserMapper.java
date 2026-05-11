package it.pagopa.selfcare.external_api.mapper;


import it.pagopa.selfcare.external_api.model.user.User;
import it.pagopa.selfcare.external_api.model.user.UserInstitution;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.utils.ProductUtils;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDetailResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Mapper(componentModel = "spring", uses = ProductService.class)
public abstract class UserMapper {

    @Autowired
    ProductService productService;

    public ProductService getProductService(){
        return this.productService;
    }

    public abstract UserInstitution toUserInstitutionsFromUserInstitutionResponse(UserInstitutionResponse userInstitutionResponse);

    public abstract User toUserFromUserDetailResponse(UserDetailResponse onboardingData);

    @Mapping(target = "productRoleLabel", expression = "java(toProductRoleLabel(onboardedProduct, getProductService().getProductRaw(onboardedProduct.getProductId())))")
    public abstract it.pagopa.selfcare.external_api.model.user.OnboardedProductResponse
    onboardedProductResponseToOnboardedProductResponse(OnboardedProductResponse onboardedProduct);

    @Named("toProductRoleLabel")
    protected String toProductRoleLabel(OnboardedProductResponse onboardedProduct, Product product) {
        ProductRole productRole = null;
        try { productRole = ProductUtils.getProductRole(onboardedProduct.getProductRole(), PartyRole.valueOf(onboardedProduct.getRole()), product); }
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
