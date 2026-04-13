package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OnboardingMsConnector {

    void onboarding(OnboardingData onboardingData);

    void onboardingUsers(OnboardingData onboardingData);

    void onboardingUsersAggregator(OnboardingData onboardingData);

    void onboardingCompany(OnboardingData onboardingData);

    void onboardingTokenComplete(String onboardingId, MultipartFile contract);

    void onboardingUsersComplete(String onboardingId, MultipartFile contract);

    void onboardingPending(String onboardingId);

    void approveOnboarding(String onboardingId);

    void rejectOnboarding(String onboardingId, String reason);

    OnboardingData getOnboarding(String onboardingId);

    OnboardingData getOnboardingWithUserInfo(String onboardingId);

    void onboardingPaAggregation(OnboardingData onboardingData);

    List<OnboardingData> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    boolean checkManager(CheckManagerData checkManagerData);

    RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode);

    VerifyAggregateResult aggregatesVerification(MultipartFile file, String productId);

    void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    void onboardingUsersPgFromIcAndAde(OnboardingData onboardingUserPgRequest);

    List<OnboardingResult> onboardingWithFilter(String taxCode, String status);
}
