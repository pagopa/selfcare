package it.pagopa.selfcare.onboarding.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.product.entity.Product;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = OnboardingWorkflowAggregator.class, name = "AGGREGATOR"),
  @JsonSubTypes.Type(value = OnboardingWorkflowInstitution.class, name = "INSTITUTION"),
  @JsonSubTypes.Type(value = OnboardingWorkflowUser.class, name = "USER"),
  @JsonSubTypes.Type(value = OnboardingWorkflowUserEa.class, name = "USER_EA")
})
public abstract class OnboardingWorkflow {

  protected static final String PDF_FORMAT_FILENAME = "%s_accordo_adesione.pdf";

  OnboardingWorkflow(Onboarding onboarding) {
    this.onboarding = onboarding;
  }

  public OnboardingWorkflow() {}

  protected Onboarding onboarding;

  public abstract String getEmailRegistrationPath(MailTemplatePathConfig config);

  public abstract String getEmailCompletionPath(MailTemplatePathConfig config);

  public abstract String getPdfFormatFilename();

  public abstract TokenType getTokenType();

  public abstract String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config);

  public abstract String getRejectTokenUrl(MailTemplatePlaceholdersConfig config);

  public abstract String getContractTemplatePath(Product product);

  public abstract String getContractTemplateVersion(Product product);

  @JsonProperty("onboarding")
  public Onboarding getOnboarding() {
    return onboarding;
  }

  @JsonProperty("onboarding")
  public void setOnboarding(Onboarding onboarding) {
    this.onboarding = onboarding;
  }
}
