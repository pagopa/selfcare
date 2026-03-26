package it.pagopa.selfcare.onboarding.connector.rest.mapper;


import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.*;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionBase")
    OnboardingPaRequest toOnboardingPaRequest(OnboardingData onboardingData);
    @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionPsp")
    OnboardingPspRequest toOnboardingPspRequest(OnboardingData onboardingData);
    @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionBase")
    @Mapping(target = "additionalInformations", source = "institutionUpdate.additionalInformations")
    @Mapping(target = "gpuData", source = "institutionUpdate.gpuData")
    OnboardingDefaultRequest toOnboardingDefaultRequest(OnboardingData onboardingData);

    @Mapping(target = "businessName", source = "institutionUpdate.description")
    @Mapping(target = "taxCode", source = "institutionUpdate.taxCode")
    @Mapping(target = "digitalAddress", source = "institutionUpdate.digitalAddress")
    OnboardingPgRequest toOnboardingPgRequest(OnboardingData onboardingData);

    GeographicTaxonomyDto toGeographicTaxonomyDto(GeographicTaxonomy geographicTaxonomy);

    @Named("toInstitutionBase")
    default InstitutionBaseRequest toInstitutionBase(OnboardingData onboardingData) {
        InstitutionBaseRequest institution = new InstitutionBaseRequest();
        institution.institutionType(InstitutionType.valueOf(onboardingData.getInstitutionType().name()));
        institution.taxCode(onboardingData.getTaxCode());
        institution.setIstatCode(onboardingData.getIstatCode());
        institution.subunitCode(onboardingData.getSubunitCode());
        institution.subunitType(Optional.ofNullable(onboardingData.getSubunitType())
                .map(InstitutionPaSubunitType::valueOf)
                .orElse(null));
        institution.setOrigin(Optional.ofNullable(onboardingData.getOrigin()).map(Origin::fromValue).orElse(null));
        if(Objects.nonNull(onboardingData.getOriginId())) {
            institution.setOriginId(onboardingData.getOriginId());
        }
        if(Objects.nonNull(onboardingData.getLocation())) {
            institution.setCity(onboardingData.getLocation().getCity());
            institution.setCountry(onboardingData.getLocation().getCountry());
            institution.setCounty(onboardingData.getLocation().getCounty());
        }
        institution.setDescription(onboardingData.getInstitutionUpdate().getDescription());
        institution.digitalAddress(onboardingData.getInstitutionUpdate().getDigitalAddress());
        institution.address(onboardingData.getInstitutionUpdate().getAddress());
        institution.zipCode(onboardingData.getInstitutionUpdate().getZipCode());
        institution.geographicTaxonomies(Optional.ofNullable(onboardingData.getInstitutionUpdate().getGeographicTaxonomies())
                .map(geotaxes -> geotaxes.stream()
                        .map(this::toGeographicTaxonomyDto)
                        .toList())
                .orElse(null));
        institution.rea(onboardingData.getInstitutionUpdate().getRea());
        institution.shareCapital(onboardingData.getInstitutionUpdate().getShareCapital());
        institution.businessRegisterPlace(onboardingData.getInstitutionUpdate().getBusinessRegisterPlace());
        institution.supportEmail(onboardingData.getInstitutionUpdate().getSupportEmail());
        institution.supportPhone(onboardingData.getInstitutionUpdate().getSupportPhone());
        institution.imported(onboardingData.getInstitutionUpdate().getImported());
        institution.setAtecoCodes(onboardingData.getAtecoCodes());
        institution.setLegalForm(onboardingData.getInstitutionUpdate().getLegalForm());
        return institution;
    }


    @Named("toInstitutionPsp")
    default InstitutionPspRequest toInstitutionPsp(OnboardingData onboardingData) {
        InstitutionPspRequest institutionPsp = new InstitutionPspRequest();
        institutionPsp.institutionType(InstitutionType.valueOf(onboardingData.getInstitutionType().name()));
        institutionPsp.taxCode(onboardingData.getTaxCode());
        institutionPsp.subunitCode(onboardingData.getSubunitCode());
        institutionPsp.subunitType(Optional.ofNullable(onboardingData.getSubunitType())
                .map(InstitutionPaSubunitType::valueOf)
                .orElse(null));
        institutionPsp.setIstatCode(onboardingData.getIstatCode());
        institutionPsp.setOrigin(Optional.ofNullable(onboardingData.getOrigin()).map(Origin::fromValue).orElse(null));
        if(Objects.nonNull(onboardingData.getOriginId())) {
            institutionPsp.setOriginId(onboardingData.getOriginId());
        }
        if(Objects.nonNull(onboardingData.getLocation())) {
            institutionPsp.setCity(onboardingData.getLocation().getCity());
            institutionPsp.setCountry(onboardingData.getLocation().getCountry());
            institutionPsp.setCounty(onboardingData.getLocation().getCounty());
        }
        institutionPsp.setDescription(onboardingData.getInstitutionUpdate().getDescription());
        institutionPsp.digitalAddress(onboardingData.getInstitutionUpdate().getDigitalAddress());
        institutionPsp.address(onboardingData.getInstitutionUpdate().getAddress());
        institutionPsp.zipCode(onboardingData.getInstitutionUpdate().getZipCode());
        institutionPsp.geographicTaxonomies(Optional.ofNullable(onboardingData.getInstitutionUpdate().getGeographicTaxonomies())
                .map(geotaxes -> geotaxes.stream()
                    .map(this::toGeographicTaxonomyDto)
                    .toList())
                .orElse(null));
        institutionPsp.rea(onboardingData.getInstitutionUpdate().getRea());
        institutionPsp.shareCapital(onboardingData.getInstitutionUpdate().getShareCapital());
        institutionPsp.businessRegisterPlace(onboardingData.getInstitutionUpdate().getBusinessRegisterPlace());
        institutionPsp.supportEmail(onboardingData.getInstitutionUpdate().getSupportEmail());
        institutionPsp.supportPhone(onboardingData.getInstitutionUpdate().getSupportPhone());
        institutionPsp.imported(onboardingData.getInstitutionUpdate().getImported());


        institutionPsp.setPaymentServiceProvider(toPaymentServiceProviderRequest(onboardingData.getInstitutionUpdate().getPaymentServiceProvider()));
        institutionPsp.setDataProtectionOfficer(toDataProtectionOfficerRequest(onboardingData.getInstitutionUpdate().getDataProtectionOfficer()));
        return institutionPsp;
    }

    PaymentServiceProviderRequest toPaymentServiceProviderRequest(PaymentServiceProvider paymentServiceProvider);
    DataProtectionOfficerRequest toDataProtectionOfficerRequest(DataProtectionOfficer dataProtectionOfficer);

    @Mapping(target = "institutionUpdate", source = "institution")
    @Mapping(target = "institutionUpdate.additionalInformations", source = "additionalInformations")
    OnboardingData toOnboardingData(OnboardingGet onboardingGet);

    @Mapping(target = "institutionUpdate", source = "institution")
    @Mapping(target = "institutionUpdate.additionalInformations", source = "additionalInformations")
    @Mapping(target = "institutionUpdate.origin", source = "institution.origin", qualifiedByName = "setOrigin")
    OnboardingData toOnboardingData(OnboardingResponse onboardingResponse);

    @Named("setOrigin")
    default String setOrigin(Origin origin) {
       return Objects.nonNull(origin) ? origin.name() : null;
    }

    OnboardingUserRequest toOnboardingUsersRequest(OnboardingData onboardingData);

    CheckManagerRequest toCheckManagerRequest(CheckManagerData checkManagerData);

    @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionBase")
    OnboardingPaRequest toOnboardingPaAggregationRequest(OnboardingData onboardingData);

    VerifyAggregateResult toVerifyAggregateResult(VerifyAggregateResponse body);

    RecipientCodeStatusResult toRecipientCodeStatusResult(RecipientCodeStatus recipientCodeStatus);

    OnboardingUserPgRequest toOnboardingUserPgRequest(OnboardingData onboardingData);

   default List<OnboardingResult> toOnboardingWithFilter(OnboardingGetResponse onboardingGetResponse) {
        if (onboardingGetResponse == null || onboardingGetResponse.getItems() == null) {
            return List.of();
        }

        return onboardingGetResponse.getItems().stream()
                .filter(Objects::nonNull)
                .map(current -> {
                    OnboardingResult onboarding = new OnboardingResult();
                    onboarding.setId(current.getId());
                    onboarding.setProductId(current.getProductId());
                    onboarding.setStatus(current.getStatus());
                    return onboarding;
                })
                .toList();
    }
}
