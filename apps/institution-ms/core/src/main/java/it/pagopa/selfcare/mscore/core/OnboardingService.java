package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.onboarding.VerifyOnboardingFilters;

public interface OnboardingService {

    void verifyOnboardingInfo(String externalId, String productId);

    void verifyOnboardingInfoSubunit(String taxCode, String subunitCode, String productId);

    void verifyOnboardingInfoByFilters(VerifyOnboardingFilters filters);

    Institution persistOnboarding(String institutionId, String productId, Onboarding onboarding, StringBuilder httpStatus);

    void deleteOnboardedInstitution(String institutionId, String productId);
}
