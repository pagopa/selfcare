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

    Uni<Void> onboardingUsers(OnboardingData onboardingData);

    Uni<Void> onboardingUsersAggregator(OnboardingData onboardingData);

    Uni<Void> onboardingCompany(OnboardingData onboardingData);

    Uni<Void> onboardingTokenComplete(String onboardingId, UploadedFile contract);

    Uni<Void> onboardingUsersComplete(String onboardingId, UploadedFile contract);

    Uni<Void> onboardingPending(String onboardingId);

    Uni<Void> approveOnboarding(String onboardingId, String userUid);

    Uni<Void> rejectOnboarding(String onboardingId, String reason, String userUid);

    Uni<OnboardingGet> getOnboarding(String onboardingId);

    Uni<OnboardingGet> getOnboardingWithUserInfo(String onboardingId);

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
