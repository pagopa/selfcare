package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.VerifyAggregateResult;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;

import java.util.List;

public interface OnboardingService {

    void onboarding(OnboardingData onboardingData);

    void onboardingUsers(OnboardingData onboardingData);

    void onboardingUsersAggregator(OnboardingData onboardingData);

    void onboardingCompany(OnboardingData onboardingData);

    void onboardingTokenComplete(String onboardingId, UploadedFile contract);

    void onboardingUsersComplete(String onboardingId, UploadedFile contract);

    void onboardingPending(String onboardingId);

    void approveOnboarding(String onboardingId, String userUid);

    void rejectOnboarding(String onboardingId, String reason, String userUid);

    OnboardingGet getOnboarding(String onboardingId);

    OnboardingGet getOnboardingWithUserInfo(String onboardingId);

    void onboardingPaAggregation(OnboardingData onboardingData);

    List<OnboardingResponse> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    boolean checkManager(CheckManagerRequest request);

    RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode);

    void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData);

    List<OnboardingResult> onboardingWithFilter(String taxCode, String status);

    VerifyAggregateResult aggregatesVerification(UploadedFile file, String productId);

    void triggerOnboardingRequest(String onboardingId);
}
