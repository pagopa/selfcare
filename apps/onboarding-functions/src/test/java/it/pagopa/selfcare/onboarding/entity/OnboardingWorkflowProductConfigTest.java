package it.pagopa.selfcare.onboarding.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import java.util.List;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
import org.openapi.quarkus.product_json.model.OnboardingType;
import org.openapi.quarkus.product_json.model.ProductResponse;

@QuarkusTest
class OnboardingWorkflowProductConfigTest {

  @Test
  void institutionAndUserWorkflowsPreferInstitutionSpecificTemplates() {
    Onboarding onboarding = onboarding(InstitutionType.PA);
    ProductResponse product = new ProductResponse();
    product.setContracts(List.of(
        contract(OnboardingType.INSTITUTION, org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT,
            "institution-default.html", "default-v1"),
        contract(OnboardingType.INSTITUTION, org.openapi.quarkus.product_json.model.InstitutionType.PA,
            "institution-pa.html", "pa-v2"),
        contract(OnboardingType.USER, org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT,
            "user-default.html", "default-v3"),
        contract(OnboardingType.USER, org.openapi.quarkus.product_json.model.InstitutionType.PA,
            "user-pa.html", "pa-v4")));

    OnboardingWorkflowInstitution institutionWorkflow = new OnboardingWorkflowInstitution(onboarding, "type");
    OnboardingWorkflowUser userWorkflow = new OnboardingWorkflowUser(onboarding, "type");

    assertEquals("institution-pa.html", institutionWorkflow.getContractTemplatePath(product));
    assertEquals("pa-v2", institutionWorkflow.getContractTemplateVersion(product));
    assertEquals("user-pa.html", userWorkflow.getContractTemplatePath(product));
    assertEquals("pa-v4", userWorkflow.getContractTemplateVersion(product));
  }

  @Test
  void institutionAndUserWorkflowsFallBackToDefaultTemplates() {
    Onboarding onboarding = onboarding(InstitutionType.PG);
    ProductResponse product = new ProductResponse();
    product.setContracts(List.of(
        contract(OnboardingType.INSTITUTION, org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT,
            "institution-default.html", "institution-default-v1"),
        contract(OnboardingType.USER, org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT,
            "user-default.html", "user-default-v1")));

    OnboardingWorkflowInstitution institutionWorkflow = new OnboardingWorkflowInstitution(onboarding, "type");
    OnboardingWorkflowUser userWorkflow = new OnboardingWorkflowUser(onboarding, "type");

    assertEquals("institution-default.html", institutionWorkflow.getContractTemplatePath(product));
    assertEquals("institution-default-v1", institutionWorkflow.getContractTemplateVersion(product));
    assertEquals("user-default.html", userWorkflow.getContractTemplatePath(product));
    assertEquals("user-default-v1", userWorkflow.getContractTemplateVersion(product));
  }

  @Test
  void workflowsReturnNullWhenNoTemplateIsConfigured() {
    Onboarding onboarding = onboarding(InstitutionType.PA);
    ProductResponse product = new ProductResponse();
    product.setContracts(List.of());

    assertNull(new OnboardingWorkflowInstitution(onboarding, "type").getContractTemplatePath(product));
    assertNull(new OnboardingWorkflowInstitution(onboarding, "type").getContractTemplateVersion(product));
    assertNull(new OnboardingWorkflowUser(onboarding, "type").getContractTemplatePath(product));
    assertNull(new OnboardingWorkflowUser(onboarding, "type").getContractTemplateVersion(product));
  }

  private static Onboarding onboarding(InstitutionType institutionType) {
    Institution institution = new Institution();
    institution.setInstitutionType(institutionType);
    Onboarding onboarding = new Onboarding();
    onboarding.setInstitution(institution);
    return onboarding;
  }

  private static ContractTemplateConfig contract(
      OnboardingType onboardingType,
      org.openapi.quarkus.product_json.model.InstitutionType institutionType,
      String path,
      String version) {
    ContractTemplateConfig config = new ContractTemplateConfig();
    config.setOnboardingType(onboardingType);
    config.setContractType(ContractType.CONTRACT);
    config.setInstitutionType(institutionType);
    config.setPath(path);
    config.setVersion(version);
    config.setEnabled(true);
    return config;
  }
}

