package it.pagopa.selfcare.onboarding.service;


import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.dto.NotificationMailRequest;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.product.entity.Product;

import java.util.List;

public interface NotificationService {

    String rejectOnboardingUrl();

    void sendMail(NotificationMailRequest notificationMailRequest);

    void sendMailRegistration(String institutionName, String destination, String name, String username, String productName, String expirationDate);

    void sendMailRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId);

    void sendMailOnboardingApprove(String institutionName, String name, String username, String productName, String onboardingId);

    void sendMailRegistrationForContract(SendMailInput sendMailInput, String expirationDate, OnboardingWorkflow onboardingWorkflow);

    void sendMailRegistrationForContract(String onboardingId, String destination, String name, String username, String productName, String institutionName, String expirationDate, OnboardingWorkflow onboardingWorkflow);

    void sendMailRegistrationForContractAggregator(String onboardingId, String destination, String name, String username, String productName, String expirationDate);

    void sendCompletedEmail(List<String> destinationMails, Product product, OnboardingWorkflow onboardingWorkflow);

    void sendDeletedEmail(List<String> destinationMails, Product product, Onboarding onboarding);

    void sendMailRejection(List<String> destinationMails, Product product, Onboarding onboarding);

    void sendCompletedEmailAggregate(String institutionName, List<String> destinationMails);

    void sendTestEmail(ExecutionContext context);
}
