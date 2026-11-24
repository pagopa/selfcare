package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingContractUpdateRequest;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserProductResponse;

public interface InstitutionV2Service {

    UserInfo getInstitutionUser(String institutionId, String userId, String loggedUserId);
    UserProductResponse getAllInstitutionUser(String institutionId, String userId, String loggedUserId);
    Institution findInstitutionById(String institutionId);
    Institution findAllInstitutionById(String institutionId);
    Boolean verifyIfExistsPendingOnboarding(String taxCode, String subunitCode, String productId);
    Boolean checkAttachmentStatus(String institutionId, String productId, String attachmentName);
    void onboardingContractUpdate(OnboardingContractUpdateRequest onboardingContractUpdateRequest);

}
