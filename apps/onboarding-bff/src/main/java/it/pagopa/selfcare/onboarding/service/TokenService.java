package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;

public interface TokenService {
  Uni<OnboardingData> verifyOnboarding(String onboardingId);

  default void approveOnboarding(String onboardingId) {
    approveOnboarding(onboardingId, null);
  }

  void approveOnboarding(String onboardingId, String userUid);

  default void rejectOnboarding(String onboardingId, String reason) {
    rejectOnboarding(onboardingId, reason, null);
  }

  void rejectOnboarding(String onboardingId, String reason, String userUid);

  OnboardingData getOnboardingWithUserInfo(String onboardingId);

  void completeTokenV2(String onboardingId, UploadedFile contract);

  void completeOnboardingUsers(String onboardingId, UploadedFile contract);

  BinaryData getContract(String onboardingId);

  BinaryData getContractSigned(String onboardingId);

  Uni<BinaryData> getTemplateAttachment(String onboardingId, String filename);

  BinaryData getAttachment(String onboardingId, String filename);

  AvailableDocuments getAvailableDocuments(String onboardingId);

  BinaryData getAggregatesCsv(String onboardingId, String productId);

  Uni<Void> uploadAttachment(String tenantId, String onboardingId, UploadedFile attachment, String attachmentName,
                        String attachmentId, String attachmentDescription);

  int headAttachment(String onboardingId, String filename);
}
