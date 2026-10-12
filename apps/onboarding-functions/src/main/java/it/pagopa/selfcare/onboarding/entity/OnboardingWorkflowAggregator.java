package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.onboarding.utils.ProductConfigUtils;
import org.openapi.quarkus.product_json.model.OnboardingType;
import org.openapi.quarkus.product_json.model.ProductResponse;

public class OnboardingWorkflowAggregator extends OnboardingWorkflow {

  private String type;

  public OnboardingWorkflowAggregator() {}

  public OnboardingWorkflowAggregator(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  @Override
  public DocumentType getDocumentType() {
    return DocumentType.INSTITUTION;
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
  public String getContractTemplatePath(ProductResponse product) {
    return ProductConfigUtils.contractTemplate(product, OnboardingType.INSTITUTION_AGGREGATOR,
        InstitutionUtils.getCurrentInstitutionType(onboarding)).map(config -> config.getPath()).orElse(null);
  }

  @Override
  public String getContractTemplateVersion(ProductResponse product) {
    return ProductConfigUtils.contractTemplate(product, OnboardingType.INSTITUTION_AGGREGATOR,
        InstitutionUtils.getCurrentInstitutionType(onboarding)).map(config -> config.getVersion()).orElse(null);
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
