package it.pagopa.selfcare.onboarding.entity;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD_GARANTITO;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.product.entity.Product;

public class OnboardingWorkflowInstitution extends OnboardingWorkflow {

  private String type;

  public OnboardingWorkflowInstitution() {}

  public OnboardingWorkflowInstitution(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  @Override
  public String getEmailRegistrationPath(MailTemplatePathConfig config) {
    return config.registrationPath();
  }

  @Override
  public String getEmailCompletionPath(MailTemplatePathConfig config) {
    if (InstitutionType.PT.equals(this.onboarding.getInstitution().getInstitutionType())) {
      return config.completePathPt();
    } else {
      return this.onboarding.getProductId().equals(PROD_FD.getValue())
              || this.onboarding.getProductId().equals(PROD_FD_GARANTITO.getValue())
          ? config.completePathFd()
          : config.completePath();
    }
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
  public String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config) {
    return config.confirmTokenPlaceholder();
  }

  @Override
  public String getRejectTokenUrl(MailTemplatePlaceholdersConfig config) {
    return config.rejectTokenPlaceholder();
  }

  @Override
  public String getContractTemplatePath(Product product) {
    return product
        .getInstitutionContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplatePath();
  }

  @Override
  public String getContractTemplateVersion(Product product) {
    return product
        .getInstitutionContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplateVersion();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
