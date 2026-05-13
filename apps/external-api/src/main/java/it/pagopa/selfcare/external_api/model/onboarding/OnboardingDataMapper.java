package it.pagopa.selfcare.external_api.model.onboarding;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class OnboardingDataMapper {

    public static OnboardingData toOnboardingData(PdaOnboardingData pdaOnboardingData) {
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setInstitutionExternalId(pdaOnboardingData.getInstitutionExternalId());
        onboardingData.setTaxCode(pdaOnboardingData.getTaxCode());
        onboardingData.setProductId(pdaOnboardingData.getProductId());
        onboardingData.setProductName(pdaOnboardingData.getProductName());
        onboardingData.setInstitutionType(pdaOnboardingData.getInstitutionType());
        onboardingData.setOrigin(pdaOnboardingData.getOrigin());
        onboardingData.setUsers(pdaOnboardingData.getUsers());
        onboardingData.setBilling(pdaOnboardingData.getBilling());
        onboardingData.setContractVersion(pdaOnboardingData.getContractVersion());
        onboardingData.setContractPath(pdaOnboardingData.getContractPath());
        InstitutionUpdate institutionUpdate = getInstitutionUpdate(pdaOnboardingData);
        onboardingData.setInstitutionUpdate(institutionUpdate);
        onboardingData.setSendCompleteOnboardingEmail(pdaOnboardingData.getSendCompleteOnboardingEmail());

        return onboardingData;
    }

    private static InstitutionUpdate getInstitutionUpdate(PdaOnboardingData pdaOnboardingData) {
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(pdaOnboardingData.getInstitutionType());
        institutionUpdate.setDescription(pdaOnboardingData.getDescription());
        institutionUpdate.setDigitalAddress(pdaOnboardingData.getDigitalAddress());
        institutionUpdate.setAddress(pdaOnboardingData.getAddress());
        institutionUpdate.setTaxCode(pdaOnboardingData.getTaxCode());
        institutionUpdate.setZipCode(pdaOnboardingData.getZipCode());
        institutionUpdate.setGeographicTaxonomies(Collections.emptyList());
        return institutionUpdate;
    }
}
