package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.user.CertifiedField;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.WorkContact;
import it.pagopa.selfcare.dashboard.model.onboarding.OnboardingRequestInfo;
import it.pagopa.selfcare.dashboard.model.institution.*;
import it.pagopa.selfcare.dashboard.model.onboarding.OnboardingRequestResource;
import it.pagopa.selfcare.dashboard.model.onboarding.OnboardingStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class OnboardingRequestMapper {

    public static OnboardingRequestResource toResource(OnboardingRequestInfo model) {
        return Optional.ofNullable(model)
                .map(onboardingRequestInfo -> {
                    final OnboardingRequestResource resource = new OnboardingRequestResource();
                    Optional.ofNullable(model.getInstitutionInfo())
                            .map(InstitutionInfo::getStatus)
                            .map(RelationshipState::toString)
                            .map(OnboardingStatus::valueOf)
                            .ifPresent(resource::setStatus);
                    resource.setInstitutionInfo(toResource(onboardingRequestInfo.getInstitutionInfo()));
                    final String institutionId = Optional.ofNullable(onboardingRequestInfo.getInstitutionInfo())
                            .map(InstitutionInfo::getId)
                            .orElse(null);
                    Optional.ofNullable(onboardingRequestInfo.getManager())
                            .ifPresent(userInfo -> resource.setManager(toResource(userInfo.getUser(), institutionId)));
                    Optional.ofNullable(onboardingRequestInfo.getAdmins())
                            .ifPresent(userInfos -> resource.setAdmins(userInfos.stream()
                                    .map(userInfo -> toResource(userInfo.getUser(), institutionId))
                                    .collect(Collectors.toList())));
                    resource.setProductId(model.getProductId());
                    return resource;
                }).orElse(null);
    }


    public static OnboardingRequestResource.InstitutionInfo toResource(InstitutionInfo model) {
        return Optional.ofNullable(model)
                .map(institutionInfo -> {
                    final OnboardingRequestResource.InstitutionInfo resource = new OnboardingRequestResource.InstitutionInfo();
                    resource.setId(model.getId());
                    resource.setInstitutionType(model.getInstitutionType());
                    resource.setName(model.getDescription());
                    resource.setFiscalCode(model.getTaxCode());
                    resource.setMailAddress(model.getDigitalAddress());
                    resource.setAddress(model.getAddress());
                    resource.setCity(model.getCity());
                    resource.setCountry(model.getCountry());
                    resource.setCounty(model.getCounty());
                    resource.setZipCode(model.getZipCode());
                    Optional.ofNullable(model.getBilling())
                            .ifPresent(billing -> {
                                resource.setRecipientCode(billing.getRecipientCode());
                                resource.setVatNumber(billing.getVatNumber());
                            });
                    resource.setPspData(toResource(model.getPaymentServiceProvider()));
                    resource.setDpoData(toResource(model.getDataProtectionOfficer()));
                    resource.setAdditionalInformations(toResource(model.getAdditionalInformations()));
                    return resource;
                }).orElse(null);
    }


    public static OnboardingRequestResource.InstitutionInfo.PspData toResource(PaymentServiceProvider model) {
        return Optional.ofNullable(model)
                .map(psp -> {
                    final OnboardingRequestResource.InstitutionInfo.PspData resource = new OnboardingRequestResource.InstitutionInfo.PspData();
                    resource.setAbiCode(psp.getAbiCode());
                    resource.setBusinessRegisterNumber(psp.getBusinessRegisterNumber());
                    resource.setLegalRegisterName(psp.getLegalRegisterName());
                    resource.setLegalRegisterNumber(psp.getLegalRegisterNumber());
                    resource.setVatNumberGroup(psp.getVatNumberGroup());
                    return resource;
                }).orElse(null);
    }


    public static OnboardingRequestResource.InstitutionInfo.DpoData toResource(DataProtectionOfficer model) {
        return Optional.ofNullable(model)
                .map(dpo -> {
                    final OnboardingRequestResource.InstitutionInfo.DpoData resource = new OnboardingRequestResource.InstitutionInfo.DpoData();
                    resource.setAddress(dpo.getAddress());
                    resource.setEmail(dpo.getEmail());
                    resource.setPec(dpo.getPec());
                    return resource;
                }).orElse(null);
    }

    public static OnboardingRequestResource.InstitutionInfo.AdditionalInformations toResource(AdditionalInformations model) {
        return Optional.ofNullable(model)
                .map(additionaInformations -> {
                    final OnboardingRequestResource.InstitutionInfo.AdditionalInformations resource = new OnboardingRequestResource.InstitutionInfo.AdditionalInformations();
                    resource.setIpa(additionaInformations.isIpa());
                    resource.setIpaCode(additionaInformations.getIpaCode());
                    resource.setOtherNote(additionaInformations.getOtherNote());
                    resource.setBelongRegulatedMarket(additionaInformations.isBelongRegulatedMarket());
                    resource.setRegulatedMarketNote(additionaInformations.getRegulatedMarketNote());
                    resource.setAgentOfPublicService(additionaInformations.isAgentOfPublicService());
                    resource.setAgentOfPublicServiceNote(additionaInformations.getAgentOfPublicServiceNote());
                    resource.setEstablishedByRegulatoryProvision(additionaInformations.isEstablishedByRegulatoryProvision());
                    resource.setEstablishedByRegulatoryProvisionNote(additionaInformations.getEstablishedByRegulatoryProvisionNote());
                    return resource;
                }).orElse(null);
    }


    public static OnboardingRequestResource.UserInfo toResource(User model, String institutionId) {
        return Optional.ofNullable(model)
                .map(user -> {
                    final OnboardingRequestResource.UserInfo resource = new OnboardingRequestResource.UserInfo();
                    resource.setId(UUID.fromString(user.getId()));
                    Optional.ofNullable(user.getName())
                            .map(CertifiedField::getValue)
                            .ifPresent(resource::setName);
                    Optional.ofNullable(user.getFamilyName())
                            .map(CertifiedField::getValue)
                            .ifPresent(resource::setSurname);
                    Optional.ofNullable(user.getFiscalCode())
                            .ifPresent(resource::setFiscalCode);
                    Optional.ofNullable(user.getWorkContact(institutionId))
                            .map(WorkContact::getEmail)
                            .map(CertifiedField::getValue)
                            .ifPresent(resource::setEmail);
                    return resource;
                }).orElse(null);
    }

}
