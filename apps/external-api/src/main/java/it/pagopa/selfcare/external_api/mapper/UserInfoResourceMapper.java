package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.onboarding.OnboardedProductResource;
import it.pagopa.selfcare.external_api.model.user.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface UserInfoResourceMapper {

    @Mapping(source = "user.name.value", target = "user.name")
    @Mapping(source = "user.email.value", target = "user.email")
    @Mapping(source = "user.familyName.value", target = "user.surname")
    UserInfoResource toResource(UserInfoWrapper userInfo);

    @Mapping(source = "userId", target = "id")
    @Mapping(source = "productDetails", target = "onboardedProductDetails", qualifiedByName = "toOnboardedProductResource")
    UserDetailsResource toResource(UserDetailsWrapper userDetails);

    @Named("toOnboardedProductResource")
    OnboardedProductResource toOnboardedProductResource(ProductDetails productDetails);

    UserInstitutionResource toUserInstitutionResource(UserInstitution entity);


    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.name", target = "name", qualifiedByName = "fromCertifiedField")
    @Mapping(source = "user.familyName", target = "surname", qualifiedByName = "fromCertifiedField")
    @Mapping(target = "email", expression = "java(fromWorkContact(userProductResponse))")
    @Mapping(source = "user.fiscalCode", target = "fiscalCode")
    UserResource toUserResource(UserProductResponse userProductResponse);

    @Named("fromCertifiedField")
    default String fromCertifiedField(CertifiedField<String> certifiedField) {
        return Objects.nonNull(certifiedField) ? certifiedField.getValue() : null;
    }

    @Named("fromWorkContact")
    default String fromWorkContact(UserProductResponse userProductResponse) {
        WorkContact workContact = userProductResponse.getUser()
                .getWorkContact(userProductResponse.getUserMailUuid());
        if(Objects.nonNull(workContact) && Objects.nonNull(workContact.getEmail())){
            return workContact.getEmail().getValue();
        }
        return null;
    }
}
