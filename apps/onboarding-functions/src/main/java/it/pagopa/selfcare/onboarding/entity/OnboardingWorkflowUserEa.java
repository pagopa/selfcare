package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.onboarding.utils.ProductConfigUtils;
import org.openapi.quarkus.product_json.model.OnboardingType;
import org.openapi.quarkus.product_json.model.ProductResponse;

public class OnboardingWorkflowUserEa extends OnboardingWorkflowUser {
  private String type;

  public OnboardingWorkflowUserEa(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  public OnboardingWorkflowUserEa() {}

  @Override
  public String getContractTemplatePath(ProductResponse product) {
    return ProductConfigUtils.contractTemplate(product, OnboardingType.USER_AGGREGATOR,
        InstitutionUtils.getCurrentInstitutionType(onboarding)).map(config -> config.getPath()).orElse(null);
  }

  @Override
  public String getContractTemplateVersion(ProductResponse product) {
    return ProductConfigUtils.contractTemplate(product, OnboardingType.USER_AGGREGATOR,
        InstitutionUtils.getCurrentInstitutionType(onboarding)).map(config -> config.getVersion()).orElse(null);
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }
}
