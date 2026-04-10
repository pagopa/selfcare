package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.BinaryData;
import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;

import java.util.List;

public interface OnboardingMsConnector {

    void onboarding(OnboardingData onboardingData);

    void onboardingUsers(OnboardingData onboardingData);

    void onboardingUsersAggregator(OnboardingData onboardingData);

    void onboardingCompany(OnboardingData onboardingData);

    void onboardingTokenComplete(String onboardingId, UploadedFile contract);

    void onboardingUsersComplete(String onboardingId, UploadedFile contract);

    void onboardingPending(String onboardingId);

    void approveOnboarding(String onboardingId);

    void rejectOnboarding(String onboardingId, String reason);

    OnboardingData getOnboarding(String onboardingId);

    OnboardingData getOnboardingWithUserInfo(String onboardingId);

    BinaryData getContract(String onboardingId);

    BinaryData getTemplateAttachment(String onboardingId, String filename);

    BinaryData getAttachment(String onboardingId, String filename);

    BinaryData getAggregatesCsv(String onboardingId, String productId);

    void onboardingPaAggregation(OnboardingData onboardingData);

    List<OnboardingData> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    boolean checkManager(CheckManagerData checkManagerData);

    RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode);

    VerifyAggregateResult aggregatesVerification(UploadedFile file, String productId);

    void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    void onboardingUsersPgFromIcAndAde(OnboardingData onboardingUserPgRequest);

    List<OnboardingResult> onboardingWithFilter(String taxCode, String status);

    void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName);

    int headAttachment(String onboardingId, String filename);
}
