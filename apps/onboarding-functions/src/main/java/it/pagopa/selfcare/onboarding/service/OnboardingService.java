package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.ManagingInstitutionSendEmail;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingAttachment;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.User;
import java.util.List;
import java.util.Optional;

public interface OnboardingService {

  String USERS_FIELD_LIST = "fiscalCode,familyName,name";
  String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";

  Optional<Onboarding> getOnboarding(String onboardingId);

  void createContract(OnboardingWorkflow onboardingWorkflow);

  void createAttachment(OnboardingAttachment onboardingAttachment);

  void saveTokenWithContract(OnboardingWorkflow onboardingWorkflow);

  void saveTokenWithAttachment(OnboardingAttachment onboardingAttachment);

  void sendMailRegistration(Onboarding onboarding);

  void sendMailRegistrationForUser(Onboarding onboarding);

  void sendMailRegistrationForUserRequester(Onboarding onboarding);

  void sendMailManagingInstitution(ManagingInstitutionSendEmail managingInstitutionEmailRequest);

  void saveVisuraForMerchant(Onboarding onboarding);

  void sendMailRegistrationForContract(OnboardingWorkflow onboardingWorkflow);

  void sendMailRegistrationForContractAggregator(Onboarding onboarding);

  void sendMailRegistrationForContractWhenApprove(OnboardingWorkflow onboardingWorkflow);

  void sendMailRegistrationApprove(Onboarding onboarding);

  void updateOnboardingExpiringDate(Onboarding onboarding);

  void sendMailOnboardingApprove(Onboarding onboarding);

  String getValidManagerId(List<User> users);

  void updateOnboardingStatus(String onboardingId, OnboardingStatus status);

  void updateOnboardingStatusAndInstanceId(String onboardingId, OnboardingStatus status, String instanceId);

  List<NotificationCountResult> countNotifications(String productId, String from, String to, ExecutionContext context);

  NotificationCountResult countNotificationsByFilters(
      String productId, String from, String to, ExecutionContext context);

  List<Onboarding> getOnboardingsToResend(ResendNotificationsFilters filters, int page, int pageSize);

  List<String> findByInstitutionAndProduct(String institutionId, String productId);
}
