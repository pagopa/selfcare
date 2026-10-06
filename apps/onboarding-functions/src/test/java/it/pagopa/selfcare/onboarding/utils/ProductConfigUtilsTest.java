package it.pagopa.selfcare.onboarding.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
import org.openapi.quarkus.product_json.model.EmailTemplateConfig;
import org.openapi.quarkus.product_json.model.OnboardingType;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;

class ProductConfigUtilsTest {

  @Test
  void roleMappingsPreferInstitutionTypeAndFallBackToDefault() {
    ProductResponse product = new ProductResponse();
    RoleMapping defaultRole = role("MANAGER", org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT, false);
    RoleMapping specificRole = role("MANAGER", org.openapi.quarkus.product_json.model.InstitutionType.PA, true);
    product.setRoleMappings(List.of(defaultRole, specificRole));

    assertTrue(ProductConfigUtils.roleMappings(product, "PA").get(PartyRole.MANAGER).getSkipUserCreation());
    assertFalse(ProductConfigUtils.roleMappings(product, "PG").get(PartyRole.MANAGER).getSkipUserCreation());
  }

  @Test
  void contractTemplatePrefersExactInstitutionType() {
    ProductResponse product = new ProductResponse();
    ContractTemplateConfig fallback = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT, "default.html");
    ContractTemplateConfig exact = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "pa.html");
    product.setContracts(List.of(fallback, exact));

    assertEquals("pa.html", ProductConfigUtils.contractTemplate(product, OnboardingType.INSTITUTION, "PA")
        .orElseThrow().getPath());
    assertEquals("default.html", ProductConfigUtils.contractTemplate(product, OnboardingType.INSTITUTION, "PG")
        .orElseThrow().getPath());
  }

  @Test
  void attachmentsUseSpecificConfigurationAndDoNotMixDefaultAttachments() {
    ProductResponse product = new ProductResponse();
    ContractTemplateConfig specificContract = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "pa.html");
    ContractTemplateConfig specificAttachment = contract(OnboardingType.INSTITUTION, ContractType.ATTACHMENT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "pa-attachment.html");
    ContractTemplateConfig defaultAttachment = contract(OnboardingType.INSTITUTION, ContractType.ATTACHMENT,
        org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT, "default-attachment.html");
    product.setContracts(List.of(specificContract, specificAttachment, defaultAttachment));

    assertEquals(List.of("pa-attachment.html"), ProductConfigUtils.attachments(
        product, OnboardingType.INSTITUTION, "PA").stream().map(a -> a.getTemplatePath()).toList());
    assertEquals(List.of("default-attachment.html"), ProductConfigUtils.attachments(
        product, OnboardingType.INSTITUTION, "PG").stream().map(a -> a.getTemplatePath()).toList());
  }

  @Test
  void emailTemplateFallsBackToDefaultOnlyWhenWorkflowIsAbsent() {
    ProductResponse product = new ProductResponse();
    EmailTemplateConfig defaultTemplate = emailTemplate(
        org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT,
        org.openapi.quarkus.product_json.model.OnboardingStatus.COMPLETED, "default.json");
    EmailTemplateConfig exactWrongStatus = emailTemplate(
        org.openapi.quarkus.product_json.model.InstitutionType.PA,
        org.openapi.quarkus.product_json.model.OnboardingStatus.PENDING, "pa-pending.json");
    product.setEmailTemplates(List.of(defaultTemplate, exactWrongStatus));

    assertTrue(ProductConfigUtils.emailTemplate(product, "PA", "IMPORT", "COMPLETED").isEmpty());
    assertEquals("default.json", ProductConfigUtils.emailTemplate(product, "PG", "IMPORT", "COMPLETED")
        .orElseThrow().getPath());
  }

  @Test
  void expirationDaysDefaultsToThirty() {
    assertEquals(30, ProductConfigUtils.expirationDays(new ProductResponse()));
  }

  private static RoleMapping role(
      String role, org.openapi.quarkus.product_json.model.InstitutionType institutionType, boolean skip) {
    RoleMapping mapping = new RoleMapping();
    mapping.setRole(role);
    mapping.setInstitutionType(institutionType);
    mapping.setSkipUserCreation(skip);
    return mapping;
  }

  private static ContractTemplateConfig contract(
      OnboardingType onboardingType, ContractType contractType,
      org.openapi.quarkus.product_json.model.InstitutionType institutionType, String path) {
    ContractTemplateConfig config = new ContractTemplateConfig();
    config.setOnboardingType(onboardingType);
    config.setContractType(contractType);
    config.setInstitutionType(institutionType);
    config.setEnabled(true);
    config.setPath(path);
    return config;
  }

  private static EmailTemplateConfig emailTemplate(
      org.openapi.quarkus.product_json.model.InstitutionType institutionType,
      org.openapi.quarkus.product_json.model.OnboardingStatus status, String path) {
    EmailTemplateConfig config = new EmailTemplateConfig();
    config.setInstitutionType(institutionType);
    config.setType(org.openapi.quarkus.product_json.model.WorkflowType.IMPORT);
    config.setStatus(status);
    config.setPath(path);
    return config;
  }
}

