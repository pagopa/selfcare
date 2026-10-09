package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
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

    Uni<Void> onboarding(OnboardingData onboardingData);

    void onboardingUsers(OnboardingData onboardingData);

    void onboardingUsersAggregator(OnboardingData onboardingData);

    Uni<Void> onboardingCompany(OnboardingData onboardingData);

    void onboardingTokenComplete(String onboardingId, UploadedFile contract);

    void onboardingUsersComplete(String onboardingId, UploadedFile contract);

    void onboardingPending(String onboardingId);

    void approveOnboarding(String onboardingId, String userUid);

    void rejectOnboarding(String onboardingId, String reason, String userUid);

    Uni<OnboardingGet> getOnboarding(String onboardingId);

    OnboardingGet getOnboardingWithUserInfo(String onboardingId);

    Uni<Void> onboardingPaAggregation(OnboardingData onboardingData);

    Uni<List<OnboardingResponse>> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    Uni<Boolean> checkManager(CheckManagerRequest request);

    Uni<RecipientCodeStatusResult> checkRecipientCode(String originId, String recipientCode);

    Uni<Void> verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    Uni<Void> onboardingUsersPgFromIcAndAde(OnboardingData onboardingData);

    Uni<List<OnboardingResult>> onboardingWithFilter(String taxCode, String status);

    Uni<VerifyAggregateResult> aggregatesVerification(UploadedFile file, String productId);

    Uni<Void> triggerOnboardingRequest(String onboardingId);
}
