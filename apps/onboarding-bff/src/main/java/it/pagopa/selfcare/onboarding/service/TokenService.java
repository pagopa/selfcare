package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;

public interface TokenService {
  Uni<OnboardingData> verifyOnboarding(String onboardingId);

  default Uni<Void> approveOnboarding(String onboardingId) {
    return approveOnboarding(onboardingId, null);
  }

  Uni<Void> approveOnboarding(String onboardingId, String userUid);

  default Uni<Void> rejectOnboarding(String onboardingId, String reason) {
    return rejectOnboarding(onboardingId, reason, null);
  }

  Uni<Void> rejectOnboarding(String onboardingId, String reason, String userUid);

  Uni<OnboardingData> getOnboardingWithUserInfo(String onboardingId);

  Uni<Void> completeTokenV2(String onboardingId, UploadedFile contract);

  Uni<Void> completeOnboardingUsers(String onboardingId, UploadedFile contract);

  Uni<BinaryData> getContract(String onboardingId);

  Uni<BinaryData> getContractSigned(String onboardingId);

  Uni<BinaryData> getTemplateAttachment(String onboardingId, String filename);

  Uni<BinaryData> getAttachment(String onboardingId, String filename);

  Uni<BinaryData> downloadDocument(String onboardingId, DownloadDocumentType type, String filename);

  Uni<AvailableDocuments> getAvailableDocuments(String onboardingId);

  Uni<BinaryData> getAggregatesCsv(String onboardingId, String productId);

  Uni<Void> uploadAttachment(String tenantId, String onboardingId, UploadedFile attachment, String attachmentName,
                        String attachmentId, String attachmentDescription);

  Uni<Integer> headAttachment(String onboardingId, String filename);
}
