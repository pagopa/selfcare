package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.AssistanceContacts;
import it.pagopa.selfcare.onboarding.connector.model.institutions.CompanyInformations;
import it.pagopa.selfcare.onboarding.connector.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.web.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring", uses = GeographicTaxonomyMapper.class)
public interface OnboardingInstitutionInfoMapper {
    @Mapping(target = "institution", expression = "java(toInstitutionData(model.getInstitution(), model.getAssistanceContacts(), model.getCompanyInformations()))")
    InstitutionOnboardingInfoResource toResource(InstitutionOnboardingData model);


    @Named("toInstitutionData")
    default InstitutionData toInstitutionData(InstitutionInfo model, AssistanceContacts contacts, CompanyInformations companyInformations) {
        return toData(model, contacts, companyInformations);
    }

    @Mapping(target = "billingData", source = "model", qualifiedByName = "toBilling")
    @Mapping(target = "city", source = "model.institutionLocation.city")
    @Mapping(target = "country", source = "model.institutionLocation.country")
    @Mapping(target = "county", source = "model.institutionLocation.county")
    InstitutionData toData(InstitutionInfo model, AssistanceContacts assistanceContacts, CompanyInformations companyInformations);

    @Named("toBilling")
    @Mapping(target = "publicServices", source = "model.billing.publicServices")
    @Mapping(target = "recipientCode", source = "model.billing.recipientCode")
    @Mapping(target = "vatNumber", source = "model.billing.vatNumber")
    @Mapping(target = "registeredOffice", source = "address")
    @Mapping(target = "businessName", source = "description")
    BillingDataResponseDto toBilling(InstitutionInfo model);

    @Named("toInstitutionResource")
    @Mapping(target = "userRole", expression = "java(mapUserRole(model.getUserRole()))")
    @Mapping(target = "id", expression = "java(mapId(model.getId()))")
    InstitutionResource toResource(InstitutionInfo model);


    AssistanceContactsResource toResource(AssistanceContacts model);

    CompanyInformationsResource toResource(CompanyInformations model);

    @Named("mapUserRole")
    default SelfCareAuthority mapUserRole(PartyRole model) {
        // Map the userRole logic here
        return model != null ? model.getSelfCareAuthority() : null;
    }

    default UUID mapId(String id) {
        return id != null ? UUID.fromString(id) : null;
    }
}
