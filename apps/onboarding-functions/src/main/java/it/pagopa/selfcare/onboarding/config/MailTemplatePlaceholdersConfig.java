package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.mail-template.placeholders.onboarding")
public interface MailTemplatePlaceholdersConfig {

    String userName();
    String userSurname();
    String managerName();
    String managerSurname();
    String previousManagerName();
    String previousManagerSurname();
    String productName();
    String institutionDescription();
    String businessName();
    String adminLink();
    String completeSelfcareName();
    String completeProductName();
    String completeSelfcarePlaceholder();

    String confirmTokenName();
    String confirmTokenPlaceholder();
    String confirmTokenUserPlaceholder();
    String rejectTokenName();
    String rejectTokenPlaceholder();
    String rejectTokenUserPlaceholder();
    String notificationProductName();
    String notificationRequesterName();
    String  notificationRequesterSurname();
    String reasonForReject();

    String rejectOnboardingUrlPlaceholder();
    String rejectOnboardingUrlValue();
    String rejectProductName();

    String expirationDate();


}
