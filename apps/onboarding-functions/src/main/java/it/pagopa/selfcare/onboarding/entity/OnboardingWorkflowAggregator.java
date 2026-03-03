package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.product.entity.Product;

public class OnboardingWorkflowAggregator extends OnboardingWorkflow {

  private String type;

  public OnboardingWorkflowAggregator() {}

  public OnboardingWorkflowAggregator(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  @Override
  public TokenType getTokenType() {
    return TokenType.INSTITUTION;
  }

  @Override
  public String getPdfFormatFilename() {
    return PDF_FORMAT_FILENAME;
  }

  @Override
  public String getEmailRegistrationPath(MailTemplatePathConfig config) {
    return config.registrationAggregatorPath();
  }

  @Override
  public String getEmailCompletionPath(MailTemplatePathConfig config) {
    return config.completePath();
  }

  @Override
  public String getContractTemplatePath(Product product) {
    return product
        .getInstitutionAggregatorContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplatePath();
  }

  @Override
  public String getContractTemplateVersion(Product product) {
    return product
        .getInstitutionAggregatorContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplateVersion();
  }

  @Override
  public String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config) {
    return config.confirmTokenPlaceholder();
  }

  @Override
  public String getRejectTokenUrl(MailTemplatePlaceholdersConfig config) {
    return config.rejectTokenPlaceholder();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
