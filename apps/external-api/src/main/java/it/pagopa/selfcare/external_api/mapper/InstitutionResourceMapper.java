package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.institution.*;
import it.pagopa.selfcare.external_api.model.onboarding.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InstitutionResourceMapper {


    @Mapping(target = "pspData", source = "paymentServiceProvider", qualifiedByName = "toPspDataResource")
    @Mapping(target = "companyInformations.rea", source = "rea")
    @Mapping(target = "companyInformations.shareCapital", source = "shareCapital")
    @Mapping(target = "companyInformations.businessRegisterPlace", source = "businessRegisterPlace")
    @Mapping(target = "assistanceContacts.supportPhone", source = "supportPhone")
    @Mapping(target = "assistanceContacts.supportEmail", source = "supportEmail")
    @Mapping(target = "dpoData", source = "dataProtectionOfficer", qualifiedByName = "toDpoDataResource")
    @Mapping(target = "rootParent", source = "rootParent", qualifiedByName = "toRootParentResource")
    InstitutionResource toResource(OnboardedInstitutionResource model);

    @Mapping(target = "geographicTaxonomies", source = "model.geographicTaxonomies", qualifiedByName = "toGeographicTaxonomyResource")
    InstitutionDetailResource toResource(Institution model);

    @Named("toPspDataResource")
    PspDataResource toResource(PaymentServiceProvider model);

    @Named("toRootParentResource")
    RootParentResource toResource(RootParent model);

    @Named("toDpoDataResource")
    DpoDataResource toResource(DataProtectionOfficer model);

    @Named("toGeographicTaxonomyResource")
    GeographicTaxonomyResource toResource(GeographicTaxonomy model);
}
