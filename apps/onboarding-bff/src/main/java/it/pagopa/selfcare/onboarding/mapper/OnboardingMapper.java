package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.CheckManagerData;
import it.pagopa.selfcare.onboarding.client.model.PaymentServiceProvider;
import it.pagopa.selfcare.onboarding.client.model.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.Institution;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboarding;
import it.pagopa.selfcare.onboarding.client.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.client.model.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.model.AggregateInstitution;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import org.openapi.quarkus.onboarding_json.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper(componentModel = "jakarta-cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
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

    it.pagopa.selfcare.onboarding.controller.request.GeographicTaxonomyDto toGeographicTaxonomyDto(it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy geographicTaxonomy);

    org.openapi.quarkus.onboarding_json.model.GeographicTaxonomyDto toGeographicTaxonomyGenerated(it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy geographicTaxonomy);

    @Named("toInstitutionBase")
    default InstitutionBaseRequest toInstitutionBase(OnboardingData onboardingData) {
        InstitutionBaseRequest institution = new InstitutionBaseRequest();
        institution.institutionType(org.openapi.quarkus.onboarding_json.model.InstitutionType.valueOf(onboardingData.getInstitutionType().name()));
        institution.taxCode(onboardingData.getTaxCode());
        institution.setIstatCode(onboardingData.getIstatCode());
        institution.subunitCode(onboardingData.getSubunitCode());
        institution.subunitType(Optional.ofNullable(onboardingData.getSubunitType())
                .map(InstitutionPaSubunitType::valueOf)
                .orElse(null));
        institution.setOrigin(Optional.ofNullable(onboardingData.getOrigin()).map(Origin::fromString).orElse(null));
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
                        .map(this::toGeographicTaxonomyGenerated)
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
        institutionPsp.institutionType(org.openapi.quarkus.onboarding_json.model.InstitutionType.valueOf(onboardingData.getInstitutionType().name()));
        institutionPsp.taxCode(onboardingData.getTaxCode());
        institutionPsp.subunitCode(onboardingData.getSubunitCode());
        institutionPsp.subunitType(Optional.ofNullable(onboardingData.getSubunitType())
                .map(InstitutionPaSubunitType::valueOf)
                .orElse(null));
        institutionPsp.setIstatCode(onboardingData.getIstatCode());
        institutionPsp.setOrigin(Optional.ofNullable(onboardingData.getOrigin()).map(Origin::fromString).orElse(null));
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
                    .map(this::toGeographicTaxonomyGenerated)
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

    PaymentServiceProviderRequest toPaymentServiceProviderRequest(it.pagopa.selfcare.onboarding.client.model.PaymentServiceProvider paymentServiceProvider);
    DataProtectionOfficerRequest toDataProtectionOfficerRequest(it.pagopa.selfcare.onboarding.client.model.DataProtectionOfficer dataProtectionOfficer);

    @Mapping(target = "institutionUpdate", source = "institution")
    @Mapping(target = "institutionUpdate.additionalInformations", source = "additionalInformations")
    OnboardingData toOnboardingData(OnboardingGet onboardingGet);

    @Mapping(target = "institutionUpdate", source = "institution")
    @Mapping(target = "institutionUpdate.additionalInformations", source = "additionalInformations")
    @Mapping(target = "origin", source = "institution.origin", qualifiedByName = "setOrigin")
    OnboardingData toOnboardingData(org.openapi.quarkus.onboarding_json.model.OnboardingResponse onboardingResponse);

    @Named("setOrigin")
    default String setOrigin(Origin origin) {
       return Objects.nonNull(origin) ? origin.name() : null;
    }

    OnboardingUserRequest toOnboardingUsersRequest(OnboardingData onboardingData);

    @Mapping(target = "userId", source = "userId")
    CheckManagerRequest toCheckManagerRequest(String userId, String taxCode, String productId);

    @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionBase")
    OnboardingPaRequest toOnboardingPaAggregationRequest(OnboardingData onboardingData);

    OnboardingUserPgRequest toOnboardingUserPgRequest(OnboardingData onboardingData);

    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }

    @Mapping(source = "billingData", target = "billing")
    @Mapping(source = "institutionLocationData", target = "location")
    @Mapping(source = "billingData.businessName", target = "institutionUpdate.description")
    @Mapping(source = "billingData.registeredOffice", target = "institutionUpdate.address")
    @Mapping(source = "pspData", target = "institutionUpdate.paymentServiceProvider")
    @Mapping(source = "pspData.dpoData", target = "institutionUpdate.dataProtectionOfficer")
    @Mapping(source = "geographicTaxonomies", target = "institutionUpdate.geographicTaxonomyCodes")
    @Mapping(source = "companyInformations.rea", target = "institutionUpdate.rea")
    @Mapping(source = "companyInformations.shareCapital", target = "institutionUpdate.shareCapital")
    @Mapping(source = "companyInformations.businessRegisterPlace", target = "institutionUpdate.businessRegisterPlace")
    @Mapping(source = "assistanceContacts.supportEmail", target = "institutionUpdate.supportEmail")
    @Mapping(source = "assistanceContacts.supportPhone", target = "institutionUpdate.supportPhone")
    @Mapping(source = "additionalInformations", target = "institutionUpdate.additionalInformations")
    @Mapping(source = "gpuData", target = "institutionUpdate.gpuData")
    @Mapping(source = "originId", target = "originId")
    @Mapping(source= "billingData.legalForm", target="institutionUpdate.legalForm")
    @Mapping(source = "userRequester", target = "userRequester")
    OnboardingData toEntity(OnboardingProductDto dto);

    VerifyManagerResponse toManagerVerification(it.pagopa.selfcare.onboarding.client.model.ManagerVerification managerVerification);

    Institution toInstitution(AggregateInstitution aggregateInstitution);

    @Mapping(source = "billingData", target = "billing")
    @Mapping(source = "billingData.businessName", target = "institutionUpdate.description")
    @Mapping(source = "billingData.taxCode", target = "institutionUpdate.taxCode")
    @Mapping(source = "billingData.digitalAddress", target = "institutionUpdate.digitalAddress")
    @Mapping(target = "origin", expression = "java(getOrigin(dto.getBillingData().isCertified()))")
    OnboardingData toEntity(CompanyOnboardingDto dto);

    @Mapping(target = "origin", expression = "java(getOrigin(dto.isCertified()))")
    OnboardingData toEntity(CompanyOnboardingUserDto dto);

    @Named("getOrigin")
    default String getOrigin(Boolean certified) {
        return Boolean.TRUE.equals(certified) ? "INFOCAMERE" : "ADE";
    }


    @Mapping(source = "institutionUpdate.description", target = "institutionInfo.name")
    @Mapping(source = "institutionUpdate.institutionType", target = "institutionInfo.institutionType")
    @Mapping(source = "institutionUpdate.digitalAddress", target = "institutionInfo.mailAddress")
    @Mapping(source = "location.country", target = "institutionInfo.country")
    @Mapping(source = "location.county", target = "institutionInfo.county")
    @Mapping(source = "location.city", target = "institutionInfo.city")
    @Mapping(source = "institutionUpdate.taxCode", target = "institutionInfo.fiscalCode")

    @Mapping(source = "billing.vatNumber", target = "institutionInfo.vatNumber")
    @Mapping(source = "billing.recipientCode", target = "institutionInfo.recipientCode")
    @Mapping(source = "institutionUpdate.paymentServiceProvider", target = "institutionInfo.pspData")
    @Mapping(source = "institutionUpdate.dataProtectionOfficer", target = "institutionInfo.dpoData")

    @Mapping(source = "users", target = "manager", qualifiedByName = "toManager")
    @Mapping(source = "users", target = "admins", qualifiedByName = "toAdmin")
    @Mapping(source = "institutionUpdate.additionalInformations", target = "institutionInfo.additionalInformations")
    OnboardingRequestResource toOnboardingRequestResource(OnboardingData onboardingData);

    OnboardingVerify toOnboardingVerify(OnboardingData onboardingData);

    @Mapping(source = "taxCode", target = "fiscalCode")
    OnboardingRequestResource.UserInfo toUserInfo(it.pagopa.selfcare.onboarding.client.model.User user);

    OnboardingData toEntity(OnboardingUserDto onboardingUser);

    @Mapping(target = "userId", source = "userId")
    CheckManagerRequest toCheckManagerRequest(CheckManagerDto checkManagerDto);

    default CheckManagerRequest toCheckManagerData(CheckManagerDto checkManagerDto) {
        return toCheckManagerRequest(checkManagerDto);
    }

    InstitutionLegalAddressResource toResource(InstitutionLegalAddressData model);

    @Named("toManager")
    default OnboardingRequestResource.UserInfo toManager(List<it.pagopa.selfcare.onboarding.client.model.User> users) {
        return users.stream().filter(user -> it.pagopa.selfcare.onboarding.client.model.RelationshipState.MANAGER.name().equals(user.getRole().name()))
                .map(this::toUserInfo)
                .findAny()
                .orElse(null);
    }

    @Named("toAdmin")
    default List<OnboardingRequestResource.UserInfo> toAdmin(List<it.pagopa.selfcare.onboarding.client.model.User> users) {
        return users.stream()
                .filter(user -> it.pagopa.selfcare.onboarding.client.model.RelationshipState.DELEGATE.name().equals(user.getRole().name()))
                .map(this::toUserInfo)
                .toList();
    }

    @Mapping(source = "id", target = "institutionId")
    @Mapping(source = "description", target = "businessName")
    @Mapping(source = "onboarding", target = "onboardings", qualifiedByName = "mapOnboardingList")
    InstitutionOnboardingResource toOnboardingResource(it.pagopa.selfcare.onboarding.client.model.Institution model);

    @Named("mapOnboardingList")
    default List<InstitutionOnboarding> mapOnboardingList(List<InstitutionOnboarding> onboardingList) {
        if (Objects.isNull(onboardingList)) {
            return new ArrayList<>();
        }

        return onboardingList.stream()
                .map(this::mapOnboarding)
                .toList();
    }

    InstitutionOnboarding mapOnboarding(InstitutionOnboarding model);

    VerifyAggregateResult toVerifyAggregateResult(VerifyAggregateResponse body);

    default VerifyAggregatesResponse toVerifyAggregatesResponse(VerifyAggregateResponse body) {
        VerifyAggregateResult mapped = toVerifyAggregateResult(body);
        VerifyAggregatesResponse response = new VerifyAggregatesResponse();
        if (mapped != null) {
            response.setAggregates(mapped.getAggregates());
            response.setErrors(
                    Optional.ofNullable(mapped.getErrors())
                            .orElse(List.of())
                            .stream()
                            .map(error -> {
                                RowErrorResponse rowErrorResponse = new RowErrorResponse();
                                rowErrorResponse.setRow(error.getRow());
                                rowErrorResponse.setCf(error.getCf());
                                rowErrorResponse.setReason(error.getReason());
                                return rowErrorResponse;
                            })
                            .toList()
            );
        }
        return response;
    }

    RecipientCodeStatusResult toRecipientCodeStatusResult(org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus recipientCodeStatus);

    default it.pagopa.selfcare.onboarding.model.RecipientCodeStatus toRecipientCodeStatus(org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus recipientCodeStatus) {
        if (recipientCodeStatus == null) {
            return null;
        }
        return it.pagopa.selfcare.onboarding.model.RecipientCodeStatus.valueOf(recipientCodeStatus.name());
    }

    default it.pagopa.selfcare.onboarding.model.RecipientCodeStatus toRecipientCodeStatus(RecipientCodeStatusResult recipientCodeStatusResult) {
        if (recipientCodeStatusResult == null) {
            return null;
        }
        return it.pagopa.selfcare.onboarding.model.RecipientCodeStatus.valueOf(recipientCodeStatusResult.name());
    }

    default List<String> toGeographicTaxonomyCodes(List<it.pagopa.selfcare.onboarding.controller.request.GeographicTaxonomyDto> geographicTaxonomies) {
        if (geographicTaxonomies == null) {
            return null;
        }
        return geographicTaxonomies.stream()
                .map(it.pagopa.selfcare.onboarding.controller.request.GeographicTaxonomyDto::getCode)
                .toList();
    }

    default <T> T map(it.pagopa.selfcare.onboarding.client.model.CertifiedField<T> certifiedField) {
        return certifiedField != null ? certifiedField.getValue() : null;
    }

    default <T> it.pagopa.selfcare.onboarding.client.model.CertifiedField<T> map(T value) {
        if (value == null) return null;
        it.pagopa.selfcare.onboarding.client.model.CertifiedField<T> certifiedField = new it.pagopa.selfcare.onboarding.client.model.CertifiedField<>();
        certifiedField.setValue(value);
        certifiedField.setCertification(it.pagopa.selfcare.onboarding.client.model.Certification.NONE);
        return certifiedField;
    }
}
