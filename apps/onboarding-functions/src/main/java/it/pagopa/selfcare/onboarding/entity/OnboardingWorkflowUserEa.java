package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.product.entity.Product;

public class OnboardingWorkflowUserEa extends OnboardingWorkflowUser {
  private String type;

  public OnboardingWorkflowUserEa(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  public OnboardingWorkflowUserEa() {}

  @Override
  public String getContractTemplatePath(Product product) {
    return product
        .getUserAggregatorContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplatePath();
  }

  @Override
  public String getContractTemplateVersion(Product product) {
    return product
        .getUserAggregatorContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplateVersion();
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
