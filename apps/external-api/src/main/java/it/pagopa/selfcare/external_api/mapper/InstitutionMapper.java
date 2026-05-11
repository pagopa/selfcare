package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.BillingResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.external_api.model.institution.Institution;
import it.pagopa.selfcare.external_api.model.institution.Onboarding;
import it.pagopa.selfcare.external_api.model.onboarding.Billing;
import it.pagopa.selfcare.external_api.model.onboarding.InstitutionOnboarding;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionInfo;
import it.pagopa.selfcare.external_api.model.onboarding.ProductInfo;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    InstitutionOnboarding toEntity(OnboardingResponse response);

    @Mapping(target = "institutionType", source = "onboardedProductResponse.institutionType", qualifiedByName = "toInstitutionType")
    @Mapping(target = "originId", source = "onboardedProductResponse.originId")
    @Mapping(target = "origin", source = "onboardedProductResponse.origin")
    @Mapping(target = "billing", source = "institutionResponse.onboarding", qualifiedByName = "setBillingData")
    OnboardedInstitutionInfo toOnboardedInstitution(it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse institutionResponse, OnboardedProductResponse onboardedProductResponse);

    @Mapping(target = "id", source = "productId")
    @Mapping(target = "status", expression = "java(onboardedProductResponse.getStatus().name())")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toOffsetDateTime")
    ProductInfo toProductInfo(OnboardedProductResponse onboardedProductResponse);

    @Named("setBillingData")
    static Billing setBillingData(List<OnboardedProductResponse> onboardings) {
        if(Objects.nonNull(onboardings) && !onboardings.isEmpty()) {
            Billing billing = null;
            BillingResponse billingResponse = onboardings.get(0).getBilling();
            if(Objects.nonNull(billingResponse)) {
                billing = new Billing();
                billing.setRecipientCode(billingResponse.getRecipientCode());
                billing.setVatNumber(billingResponse.getVatNumber());
                billing.setPublicServices(billingResponse.getPublicServices());
            }
            return billing;
        }
        return null;
    }

    @Named("toInstitutionType")
    default InstitutionType toInstitutionType(OnboardedProductResponse.InstitutionTypeEnum institutionTypeEnum) {
        return Optional.ofNullable(institutionTypeEnum)
                .map(enumVal -> {
                    try {
                        return InstitutionType.valueOf(enumVal.name());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);
    }



    @Named("toOffsetDateTime")
    default OffsetDateTime map(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(time -> time.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .orElse(null);
    }

    @Mapping(target = "onboarding", source = "onboarding", qualifiedByName = "toOnboardingWithDate")
    Institution toInstitution(it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse body);

    @Named("toOnboardingWithDate")
    default Onboarding toOnboardingWithDate(OnboardedProductResponse onboardedProductResponse) {
        Onboarding onboarding = toOnboardingWithoutDate(onboardedProductResponse);
        onboarding.setCreatedAt(convertDate(onboardedProductResponse.getCreatedAt()));
        onboarding.setUpdatedAt(convertDate(onboardedProductResponse.getUpdatedAt()));
        return onboarding;
    }

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    Onboarding toOnboardingWithoutDate(OnboardedProductResponse onboardedProductResponse);

    default OffsetDateTime convertDate(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(time -> time.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .orElse(null);
    }
}