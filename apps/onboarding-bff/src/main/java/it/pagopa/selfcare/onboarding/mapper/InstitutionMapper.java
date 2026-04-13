package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.model.UserAuthority;
import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;
import org.openapi.quarkus.user_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.model.OnboardedProductState;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "jakarta-cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InstitutionMapper {

    @Mapping(target = "companyInformations", source = ".", qualifiedByName = "toCompanyInformationsEntity")
    @Mapping(target = "assistanceContacts", source = ".", qualifiedByName = "toAssistanceContacts")
    Institution toEntity(InstitutionResponse dto);

    @Named("toCompanyInformationsEntity")
    default CompanyInformations toCompanyInformationsEntity(InstitutionResponse dto) {
        CompanyInformations companyInformations = new CompanyInformations();
        companyInformations.setRea(dto.getRea());
        companyInformations.setShareCapital(dto.getShareCapital());
        companyInformations.setBusinessRegisterPlace(dto.getBusinessRegisterPlace());
        return companyInformations;
    }

    @Mapping(target = "id", source = "institutionId")
    InstitutionInfo toInstitutionInfo(BillingDataResponse model);

    @Named("toAssistanceContacts")
    default AssistanceContacts toAssistanceContacts(InstitutionResponse dto) {
        AssistanceContacts assistanceContacts = new AssistanceContacts();
        assistanceContacts.setSupportEmail(dto.getSupportEmail());
        assistanceContacts.setSupportPhone(dto.getSupportPhone());
        return assistanceContacts;
    }

    OnboardingResource toResource(OnboardingResponse response);

    @Mapping(target = "id", source = "institutionId")
    @Mapping(target = "description", source = "institutionDescription")
    @Mapping(target = "userRole", source = ".", qualifiedByName = "toPartyRole")
    @Mapping(target = "status", source = ".", qualifiedByName = "toStatus")
    InstitutionInfo toInstitutionInfo(UserInstitutionResponse model);

    @Named("toPartyRole")
    default PartyRole toPartyRole(UserInstitutionResponse model) {
        try {
            return model.getProducts().stream()
                    .filter(product -> Objects.nonNull(product.getRole()))
                    .map(product -> PartyRole.valueOf(product.getRole().name()))
                    .reduce((role1,role2) -> Collections.min(List.of(role1, role2)))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("toStatus")
    default String toStatus(UserInstitutionResponse model) {
        try {
            return model.getProducts().stream()
                    .filter(product -> Objects.nonNull(product.getRole()))
                    .reduce((product1,product2) -> product1.getRole().equals(Collections.min(List.of(product1.getRole(), product2.getRole())))
                        ? product1
                        : product2)
                    .map(OnboardedProductResponse::getStatus)
                    .map(OnboardedProductState::value)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    @Mapping(target = "userRole", source = "userRole", qualifiedByName = "toUserAuthority")
    InstitutionResource toResource(InstitutionInfo model);

    InstitutionResourceIC toResource(InstitutionInfoIC model);

    BusinessResourceIC toResource(BusinessInfoIC model);

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    @Mapping(target = "institutionType", source = "institutionType", qualifiedByName = "enumToString")
    InstitutionResource toResource(Institution model);

    MatchInfoResultResource toResource(MatchInfoResult model);

    @Named("stringToUuid")
    default UUID stringToUuid(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    @Named("enumToString")
    default String enumToString(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : null;
    }

    GeographicTaxonomyResource toResource(GeographicTaxonomy model);

    ProductResource toResource(it.pagopa.selfcare.product.entity.Product model);

    OriginResponse toOriginResponse(OriginResult originEntries);

    @Mapping(target = "institution", source = ".")
    InstitutionOnboardingInfoResource toResource(InstitutionOnboardingData model);

    @Mapping(target = "billingData", source = "institution", qualifiedByName = "toBilling")
    @Mapping(target = "city", source = "institution.institutionLocation.city")
    @Mapping(target = "country", source = "institution.institutionLocation.country")
    @Mapping(target = "county", source = "institution.institutionLocation.county")
    @Mapping(target = "assistanceContacts", source = "assistanceContacts")
    @Mapping(target = "companyInformations", source = "companyInformations")
    InstitutionData toInstitutionData(InstitutionOnboardingData model);

    @Named("toBilling")
    @Mapping(target = "publicServices", source = "model.billing.publicServices")
    @Mapping(target = "recipientCode", source = "model.billing.recipientCode")
    @Mapping(target = "vatNumber", source = "model.billing.vatNumber")
    @Mapping(target = "registeredOffice", source = "address")
    @Mapping(target = "businessName", source = "description")
    BillingDataResponseDto toBilling(InstitutionInfo model);

    AssistanceContactsResource toResource(AssistanceContacts model);

    CompanyInformationsResource toResource(CompanyInformations model);

    @Named("toUserAuthority")
    default UserAuthority mapUserRole(PartyRole model) {
        if (model == null) {
            return null;
        }
        return switch (model) {
            case MANAGER, DELEGATE, SUB_DELEGATE -> UserAuthority.ADMIN;
            case OPERATOR -> UserAuthority.LIMITED;
            case ADMIN_EA -> UserAuthority.ADMIN_EA;
        };
    }

    default UUID mapId(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    InstitutionSeed toInstitutionSeed(OnboardingData onboardingData);

    InstitutionInfo toInstitutionInfo(Institution model);

    default Institution toInstitution(org.openapi.quarkus.onboarding_json.model.InstitutionResponse model) {
        if (model == null) {
            return null;
        }
        Institution institution = new Institution();
        institution.setId(model.getId());
        institution.setDescription(model.getDescription());
        institution.setTaxCode(model.getTaxCode());
        institution.setDigitalAddress(model.getDigitalAddress());
        institution.setAddress(model.getAddress());
        institution.setZipCode(model.getZipCode());
        institution.setOrigin(model.getOrigin() != null ? model.getOrigin().name() : null);
        institution.setOriginId(model.getOriginId());
        institution.setSubunitCode(model.getSubunitCode());
        institution.setSubunitType(model.getSubunitType() != null ? model.getSubunitType().name() : null);
        institution.setCity(model.getCity());
        institution.setCounty(model.getCounty());
        institution.setCountry(model.getCountry());
        if (model.getInstitutionType() != null) {
            institution.setInstitutionType(it.pagopa.selfcare.onboarding.common.InstitutionType.valueOf(model.getInstitutionType()));
        }
        return institution;
    }
}
