package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;
import org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;

import java.util.List;

public interface OnboardingService {

    void onboarding(OnboardingData onboardingData);

    VerifyAggregateResponse aggregatesVerification(UploadedFile file, String productId);

    void onboardingUsers(OnboardingData onboardingData);

    void onboardingUsersAggregator(OnboardingData onboardingData);

    void onboardingCompany(OnboardingData onboardingData);

    void onboardingTokenComplete(String onboardingId, UploadedFile contract);

    void onboardingUsersComplete(String onboardingId, UploadedFile contract);

    void onboardingPending(String onboardingId);

    void approveOnboarding(String onboardingId);

    void rejectOnboarding(String onboardingId, String reason);

    OnboardingGet getOnboarding(String onboardingId);

    OnboardingGet getOnboardingWithUserInfo(String onboardingId);

    BinaryData getContract(String onboardingId);

    BinaryData getTemplateAttachment(String onboardingId, String filename);

    BinaryData getAttachment(String onboardingId, String filename);

    BinaryData getAggregatesCsv(String onboardingId, String productId);

    void onboardingPaAggregation(OnboardingData onboardingData);

    List<OnboardingResponse> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    CheckManagerResponse checkManager(CheckManagerRequest request);

    RecipientCodeStatus checkRecipientCode(String originId, String recipientCode);

    void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData);

    OnboardingGetResponse onboardingWithFilter(String taxCode, String status);

    void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName);

    int headAttachment(String onboardingId, String filename);
}
