package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.onboarding.OnboardingData;

public interface TokenService {
  OnboardingData verifyOnboarding(String onboardingId);

  void approveOnboarding(String onboardingId);

  void rejectOnboarding(String onboardingId, String reason);

  OnboardingData getOnboardingWithUserInfo(String onboardingId);

  void completeTokenV2(String onboardingId, UploadedFile contract);

  void completeOnboardingUsers(String onboardingId, UploadedFile contract);

  BinaryData getContract(String onboardingId);

  BinaryData getTemplateAttachment(String onboardingId, String filename);

  BinaryData getAttachment(String onboardingId, String filename);

  BinaryData getAggregatesCsv(String onboardingId, String productId);

  boolean verifyAllowedUserByRole(String onboardingId, String uid);

  void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName);

  int headAttachment(String onboardingId, String filename);
}
