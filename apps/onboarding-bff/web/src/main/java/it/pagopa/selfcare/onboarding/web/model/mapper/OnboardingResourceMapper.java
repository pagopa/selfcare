package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.institutions.ManagerVerification;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionOnboarding;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.web.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OnboardingResourceMapper {

    @Mapping(source = "billingData", target = "billing")
    @Mapping(source = "institutionLocationData", target = "location")
    @Mapping(source = "billingData.businessName", target = "institutionUpdate.description")
    @Mapping(source = "billingData.registeredOffice", target = "institutionUpdate.address")
    @Mapping(source = "pspData", target = "institutionUpdate.paymentServiceProvider")
    @Mapping(source = "pspData.dpoData", target = "institutionUpdate.dataProtectionOfficer")
    @Mapping(source = "geographicTaxonomies", target = "institutionUpdate.geographicTaxonomies")
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

    VerifyManagerResponse toManagerVerification(ManagerVerification managerVerification);

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
    OnboardingRequestResource.UserInfo toUserInfo(User user);

    OnboardingData toEntity(OnboardingUserDto onboardingUser);

    CheckManagerData toCheckManagerData(CheckManagerDto checkManagerDto);

    InstitutionLegalAddressResource toResource(InstitutionLegalAddressData model);

    @Named("toManager")
    default OnboardingRequestResource.UserInfo toManager(List<User> users) {
        return users.stream().filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                .map(this::toUserInfo)
                .findAny()
                .orElse(null);
    }

    @Named("toAdmin")
    default List<OnboardingRequestResource.UserInfo> toAdmin(List<User> users) {
        return users.stream()
                .filter(user -> PartyRole.DELEGATE.equals(user.getRole()))
                .map(this::toUserInfo)
                .toList();
    }

    VerifyAggregatesResponse toVerifyAggregatesResponse(VerifyAggregateResult verifyAggregateResult);

    RecipientCodeStatus toRecipientCodeStatus(RecipientCodeStatusResult recipientCodeStatusResult);

    @Mapping(source = "id", target = "institutionId")
    @Mapping(source = "description", target = "businessName")
    @Mapping(source = "onboarding", target = "onboardings", qualifiedByName = "mapOnboardingList")
    InstitutionOnboardingResource toOnboardingResource(Institution model);

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
}
