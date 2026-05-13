package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.external_api.model.institution.Institution;
import it.pagopa.selfcare.external_api.model.institution.Onboarding;
import it.pagopa.selfcare.external_api.model.onboarding.*;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.*;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.GeographicTaxonomyDto;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

  @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionBase")
  OnboardingPaRequest toOnboardingPaRequest(OnboardingData onboardingData);

  @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionPsp")
  OnboardingPspRequest toOnboardingPspRequest(OnboardingData onboardingData);

  @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionPsp")
  @Mapping(
      target = "contractImported.activatedAt",
      source = "contractImported.activatedAt",
      qualifiedByName = "convertDate")
  @Mapping(
      target = "contractImported.createdAt",
      source = "contractImported.createdAt",
      qualifiedByName = "convertDate")
  OnboardingImportPspRequest toOnboardingImportPspRequest(OnboardingData onboardingData);

  @Mapping(target = "institution", source = ".", qualifiedByName = "toInstitutionBase")
  OnboardingDefaultRequest toOnboardingDefaultRequest(OnboardingData onboardingData);

  @Mapping(target = "institution", expression = "java(toInstitutionImportRequest(onboardingData, onboardingData.getInstitutionUpdate()))")
  @Mapping(
      target = "contractImported.createdAt",
      source = "contractImported.createdAt",
      qualifiedByName = "convertDate")
  @Mapping(
      target = "contractImported.activatedAt",
      source = "contractImported.activatedAt",
      qualifiedByName = "convertDate")
  @Mapping(target="sendMailForImport", source="sendCompleteOnboardingEmail")
  OnboardingImportRequest mapToOnboardingImportRequest(OnboardingData onboardingData);



  @Named("toInstitutionImportRequest")
  @Mapping(target="institutionType", ignore = true)
  @Mapping(target="taxCode", source="institutionUpdate.taxCode")
  @Mapping(target = "origin", expression = "java(originIpaIfAbsent(onboardingData))")
  InstitutionImportRequest toInstitutionImportRequest(OnboardingData onboardingData, InstitutionUpdate institutionUpdate);

  default Origin originIpaIfAbsent(OnboardingData onboardingData) {
    if(Objects.isNull(onboardingData.getOrigin())) {
      return Origin.IPA;
    }

    return Origin.fromValue(onboardingData.getOrigin());
  }

  GeographicTaxonomyDto toGeographicTaxonomyDto(GeographicTaxonomy geographicTaxonomy);

  @Named("convertDate")
  default LocalDateTime convertDate(OffsetDateTime date) {
    return date != null ? date.toLocalDateTime() : LocalDateTime.now();
  }

  @Named("toInstitutionBase")
  default InstitutionBaseRequest toInstitutionBase(OnboardingData onboardingData) {
    InstitutionBaseRequest institution = new InstitutionBaseRequest();
    institution.institutionType(
        InstitutionType.valueOf(onboardingData.getInstitutionType().name()));
    institution.taxCode(onboardingData.getTaxCode());
    institution.subunitCode(onboardingData.getSubunitCode());
    institution.subunitType(
        Optional.ofNullable(onboardingData.getSubunitType())
            .map(InstitutionPaSubunitType::valueOf)
            .orElse(null));
    institution.setOrigin(
        Optional.ofNullable(onboardingData.getOrigin()).map(Origin::fromValue).orElse(null));
    institution.setOriginId(onboardingData.getOriginId());
    if (Objects.nonNull(onboardingData.getLocation())) {
      institution.setCity(onboardingData.getLocation().getCity());
      institution.setCountry(onboardingData.getLocation().getCountry());
      institution.setCounty(onboardingData.getLocation().getCounty());
    }
    institution.setDescription(onboardingData.getInstitutionUpdate().getDescription());
    institution.digitalAddress(onboardingData.getInstitutionUpdate().getDigitalAddress());
    institution.address(onboardingData.getInstitutionUpdate().getAddress());
    institution.zipCode(onboardingData.getInstitutionUpdate().getZipCode());
    institution.geographicTaxonomies(
        Optional.ofNullable(onboardingData.getInstitutionUpdate().getGeographicTaxonomies())
            .map(
                geotaxes ->
                    geotaxes.stream()
                        .map(this::toGeographicTaxonomyDto)
                        .toList())
            .orElse(null));
    institution.rea(onboardingData.getInstitutionUpdate().getRea());
    institution.shareCapital(onboardingData.getInstitutionUpdate().getShareCapital());
    institution.businessRegisterPlace(
        onboardingData.getInstitutionUpdate().getBusinessRegisterPlace());
    institution.supportEmail(onboardingData.getInstitutionUpdate().getSupportEmail());
    institution.supportPhone(onboardingData.getInstitutionUpdate().getSupportPhone());
    institution.imported(onboardingData.getInstitutionUpdate().getImported());
    institution.setIstatCode(onboardingData.getIstatCode());
    return institution;
  }

  @Named("toInstitutionPsp")
  default InstitutionPspRequest toInstitutionPsp(OnboardingData onboardingData) {
    InstitutionPspRequest institutionPsp = new InstitutionPspRequest();
    institutionPsp.institutionType(
        InstitutionType.valueOf(onboardingData.getInstitutionType().name()));
    institutionPsp.taxCode(onboardingData.getTaxCode());
    institutionPsp.subunitCode(onboardingData.getSubunitCode());
    institutionPsp.subunitType(
        Optional.ofNullable(onboardingData.getSubunitType())
            .map(InstitutionPaSubunitType::valueOf)
            .orElse(null));

    institutionPsp.setOrigin(
        Optional.ofNullable(onboardingData.getOrigin()).map(Origin::fromValue).orElse(null));
    institutionPsp.setOriginId(onboardingData.getOriginId());
    if (Objects.nonNull(onboardingData.getLocation())) {
      institutionPsp.setCity(onboardingData.getLocation().getCity());
      institutionPsp.setCountry(onboardingData.getLocation().getCountry());
      institutionPsp.setCounty(onboardingData.getLocation().getCounty());
    }
    institutionPsp.setDescription(onboardingData.getInstitutionUpdate().getDescription());
    institutionPsp.digitalAddress(onboardingData.getInstitutionUpdate().getDigitalAddress());
    institutionPsp.address(onboardingData.getInstitutionUpdate().getAddress());
    institutionPsp.zipCode(onboardingData.getInstitutionUpdate().getZipCode());
    institutionPsp.geographicTaxonomies(
        Optional.ofNullable(onboardingData.getInstitutionUpdate().getGeographicTaxonomies())
            .map(
                geotaxes ->
                    geotaxes.stream()
                        .map(this::toGeographicTaxonomyDto)
                        .toList())
            .orElse(null));
    institutionPsp.rea(onboardingData.getInstitutionUpdate().getRea());
    institutionPsp.shareCapital(onboardingData.getInstitutionUpdate().getShareCapital());
    institutionPsp.businessRegisterPlace(
        onboardingData.getInstitutionUpdate().getBusinessRegisterPlace());
    institutionPsp.supportEmail(onboardingData.getInstitutionUpdate().getSupportEmail());
    institutionPsp.supportPhone(onboardingData.getInstitutionUpdate().getSupportPhone());
    institutionPsp.imported(onboardingData.getInstitutionUpdate().getImported());

    institutionPsp.setPaymentServiceProvider(
        toPaymentServiceProviderRequest(
            onboardingData.getInstitutionUpdate().getPaymentServiceProvider()));
    institutionPsp.setDataProtectionOfficer(
        toDataProtectionOfficerRequest(
            onboardingData.getInstitutionUpdate().getDataProtectionOfficer()));
    institutionPsp.setIstatCode(onboardingData.getIstatCode());
    return institutionPsp;
  }

  PaymentServiceProviderRequest toPaymentServiceProviderRequest(
      PaymentServiceProvider paymentServiceProvider);

  DataProtectionOfficerRequest toDataProtectionOfficerRequest(
      DataProtectionOfficer dataProtectionOfficer);

  @Mapping(
      target = "createdAt",
      expression =
          "java(institutionResponse.getCreatedAt() != null ? institutionResponse.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
  @Mapping(
      target = "updatedAt",
      expression =
          "java(institutionResponse.getUpdatedAt() != null ? institutionResponse.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
  @Mapping(
      target = "onboarding",
      expression = "java(toOnboardingData(institutionResponse.getOnboarding()))")
  Institution toInstitution(
      it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse institutionResponse);

  @Named("toOnboardingData")
  default List<Onboarding> toOnboardingData(List<OnboardedProductResponse> onboarding) {
    return onboarding.stream()
        .map(
            onboardedProductResponse -> {
              Onboarding onb = new Onboarding();
              onb.setProductId(onboardedProductResponse.getProductId());
              onb.setBilling(toBilling(onboardedProductResponse.getBilling()));
              return onb;
            })
        .toList();
  }

  Billing toBilling(it.pagopa.selfcare.core.generated.openapi.v1.dto.BillingResponse billing);

  @Mapping(
      target = "onboardingImportContract",
      expression = "java(toOnboardingImportContract(onboardingAggregatorImportDto))")
  OnboardingAggregatorImportData mapToOnboardingAggregatorImportRequest(
      OnboardingAggregatorImportDto onboardingAggregatorImportDto);

  default OnboardingImportContract toOnboardingImportContract(
      OnboardingAggregatorImportDto onboardingData) {
    ImportContractDto importContract = onboardingData.getImportContract();
    OnboardingImportContract onboarding = new OnboardingImportContract();
    onboarding.setCreatedAt(OffsetDateTime.now());
    onboarding.setActivatedAt(importContract.getOnboardingDate());
    onboarding.setFileName(importContract.getFileName());
    onboarding.setFilePath(importContract.getFilePath());
    onboarding.setContractType(importContract.getContractType());

    return onboarding;
  }
}
