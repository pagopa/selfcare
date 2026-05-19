package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailBuilder {

  private final MailTemplatePathConfig mailTemplatePathConfig;
  private final MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig;

  @Inject
  public MailBuilder(
    MailTemplatePathConfig mailTemplatePathConfig,
    MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig) {
    this.mailTemplatePathConfig = mailTemplatePathConfig;
    this.mailTemplatePlaceholdersConfig = mailTemplatePlaceholdersConfig;
  }

  public String rejectOnboardingUrl() {
    return mailTemplatePlaceholdersConfig.rejectOnboardingUrlValue();
  }

  public String registrationTemplatePath(OnboardingWorkflow onboardingWorkflow) {
    return onboardingWorkflow.getEmailRegistrationPath(mailTemplatePathConfig);
  }

  public String confirmTokenUrl(OnboardingWorkflow onboardingWorkflow) {
    return onboardingWorkflow.getConfirmTokenUrl(mailTemplatePlaceholdersConfig);
  }
}
