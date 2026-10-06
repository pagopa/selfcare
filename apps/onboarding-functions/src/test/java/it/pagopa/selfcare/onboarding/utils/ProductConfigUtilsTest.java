package it.pagopa.selfcare.onboarding.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import java.util.List;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
import org.openapi.quarkus.product_json.model.EmailTemplateConfig;
import org.openapi.quarkus.product_json.model.Features;
import org.openapi.quarkus.product_json.model.OnboardingType;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;

@QuarkusTest
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
  void roleMappingsFailWhenProductHasNoMatchingOrDefaultConfiguration() {
    ProductResponse product = new ProductResponse();
    product.setProductId("prod-id");
    product.setRoleMappings(List.of(role("MANAGER", org.openapi.quarkus.product_json.model.InstitutionType.PA, false)));

    assertThrows(IllegalStateException.class, () -> ProductConfigUtils.roleMappings(product, "PG"));
    assertThrows(IllegalStateException.class, () -> ProductConfigUtils.roleMappings(new ProductResponse(), "PA"));
    assertThrows(IllegalStateException.class, () -> ProductConfigUtils.roleMappings(null, "PA"));
  }

  @Test
  void roleMappingsIgnoreNullRolesAndKeepFirstDuplicate() {
    ProductResponse product = new ProductResponse();
    RoleMapping nullRole = role(null, org.openapi.quarkus.product_json.model.InstitutionType.PA, false);
    RoleMapping first = role("MANAGER", org.openapi.quarkus.product_json.model.InstitutionType.PA, false);
    RoleMapping duplicate = role("MANAGER", org.openapi.quarkus.product_json.model.InstitutionType.PA, true);
    product.setRoleMappings(List.of(nullRole, first, duplicate));

    assertEquals(first, ProductConfigUtils.roleMappings(product, "PA").get(PartyRole.MANAGER));
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
  void contractTemplateSkipsDisabledEntriesAndReturnsEmptyWhenNoMatchExists() {
    ProductResponse product = new ProductResponse();
    ContractTemplateConfig disabledSpecific = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "disabled.html");
    disabledSpecific.setEnabled(false);
    ContractTemplateConfig defaultContract = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT, "default.html");
    ContractTemplateConfig wrongWorkflow = contract(OnboardingType.USER, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "user.html");
    product.setContracts(List.of(disabledSpecific, defaultContract, wrongWorkflow));

    assertEquals("default.html", ProductConfigUtils.contractTemplate(product, OnboardingType.INSTITUTION, "PA")
        .orElseThrow().getPath());
    assertTrue(ProductConfigUtils.contractTemplate(product, OnboardingType.USER, "PG").isEmpty());
    assertTrue(ProductConfigUtils.contractTemplate(new ProductResponse(), OnboardingType.USER, "PG").isEmpty());
    assertTrue(ProductConfigUtils.contractTemplate(null, OnboardingType.USER, "PG").isEmpty());
  }

  @Test
  void contractTemplateSkipsNullAndIncompleteEntriesAndUsesNullInstitutionAsDefault() {
    ProductResponse product = new ProductResponse();
    ContractTemplateConfig missingContractType = contract(OnboardingType.INSTITUTION, null,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "incomplete-type.html");
    ContractTemplateConfig missingOnboardingType = contract(null, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "incomplete-workflow.html");
    ContractTemplateConfig defaultContract = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        null, "default.html");
    product.setContracts(java.util.Arrays.asList(null, missingContractType, missingOnboardingType, defaultContract));

    assertEquals("default.html", ProductConfigUtils.contractTemplate(product, OnboardingType.INSTITUTION, "PA")
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
  void attachmentsIgnoreNullAndIncompleteConfigurationsWhenFallingBackToDefault() {
    ProductResponse product = new ProductResponse();
    ContractTemplateConfig defaultContract = contract(OnboardingType.INSTITUTION, ContractType.CONTRACT,
        org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT, "default-contract.html");
    ContractTemplateConfig incompleteAttachment = contract(null, ContractType.ATTACHMENT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, "incomplete.pdf");
    ContractTemplateConfig defaultAttachment = contract(OnboardingType.INSTITUTION, ContractType.ATTACHMENT,
        org.openapi.quarkus.product_json.model.InstitutionType.DEFAULT, "default.pdf");
    product.setContracts(java.util.Arrays.asList(null, defaultContract, incompleteAttachment, defaultAttachment));

    assertEquals(List.of("default.pdf"), ProductConfigUtils.attachments(
        product, OnboardingType.INSTITUTION, "PA").stream().map(a -> a.getTemplatePath()).toList());
  }

  @Test
  void attachmentsAreSortedAndKeepTheDurablePayloadFields() {
    ProductResponse product = new ProductResponse();
    ContractTemplateConfig later = attachment("later.pdf", 2);
    ContractTemplateConfig first = attachment("first.pdf", 1);
    first.setName("Identity document");
    first.setMandatory(true);
    first.setGenerated(true);
    first.setVersion("v2");
    first.setWorkflowType(List.of(org.openapi.quarkus.product_json.model.WorkflowType.IMPORT));
    first.setWorkflowState("REQUEST");
    ContractTemplateConfig disabled = attachment("disabled.pdf", 0);
    disabled.setEnabled(false);
    product.setContracts(List.of(later, disabled, first));

    var attachments = ProductConfigUtils.attachments(product, OnboardingType.INSTITUTION, "PA");

    assertEquals(List.of("first.pdf", "later.pdf"), attachments.stream().map(a -> a.getTemplatePath()).toList());
    assertEquals("v2", attachments.get(0).getTemplateVersion());
    assertEquals("Identity document", attachments.get(0).getName());
    assertTrue(attachments.get(0).isMandatory());
    assertTrue(attachments.get(0).isGenerated());
    assertEquals(1, attachments.get(0).getOrder());
    assertEquals(List.of(it.pagopa.selfcare.onboarding.common.WorkflowType.IMPORT),
        attachments.get(0).getWorkflowType());
    assertEquals(it.pagopa.selfcare.onboarding.common.OnboardingStatus.REQUEST,
        attachments.get(0).getWorkflowState());
    assertTrue(ProductConfigUtils.attachments(null, OnboardingType.INSTITUTION, "PA").isEmpty());
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
  void emailTemplateSelectsExactMatchAndHandlesMissingInputs() {
    ProductResponse product = new ProductResponse();
    EmailTemplateConfig exact = emailTemplate(
        org.openapi.quarkus.product_json.model.InstitutionType.PA,
        org.openapi.quarkus.product_json.model.OnboardingStatus.COMPLETED, "pa-completed.json");
    product.setEmailTemplates(List.of(exact));

    assertEquals("pa-completed.json", ProductConfigUtils.emailTemplate(product, "PA", "IMPORT", "COMPLETED")
        .orElseThrow().getPath());
    assertTrue(ProductConfigUtils.emailTemplate(null, "PA", "IMPORT", "COMPLETED").isEmpty());
    assertTrue(ProductConfigUtils.emailTemplate(product, null, "IMPORT", "COMPLETED").isEmpty());
    assertTrue(ProductConfigUtils.emailTemplate(product, "PA", null, "COMPLETED").isEmpty());
    assertTrue(ProductConfigUtils.emailTemplate(product, "PA", "IMPORT", null).isEmpty());
    assertTrue(ProductConfigUtils.emailTemplate(new ProductResponse(), "PA", "IMPORT", "COMPLETED").isEmpty());
  }

  @Test
  void emailTemplateIgnoresExactWorkflowTemplatesWithoutAStatus() {
    ProductResponse product = new ProductResponse();
    product.setEmailTemplates(List.of(emailTemplate(
        org.openapi.quarkus.product_json.model.InstitutionType.PA, null, "missing-status.json")));

    assertTrue(ProductConfigUtils.emailTemplate(product, "PA", "IMPORT", "COMPLETED").isEmpty());
  }

  @Test
  void expirationDaysDefaultsToThirty() {
    assertEquals(30, ProductConfigUtils.expirationDays(new ProductResponse()));
    assertEquals(30, ProductConfigUtils.expirationDays(null));
    ProductResponse productWithoutExpiration = new ProductResponse();
    productWithoutExpiration.setFeatures(new Features());
    assertEquals(30, ProductConfigUtils.expirationDays(productWithoutExpiration));
  }

  @Test
  void expirationDaysUsesConfiguredValue() {
    ProductResponse product = new ProductResponse();
    Features features = new Features();
    features.setExpirationDays(45);
    product.setFeatures(features);

    assertEquals(45, ProductConfigUtils.expirationDays(product));
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

  private static ContractTemplateConfig attachment(String path, Integer order) {
    ContractTemplateConfig config = contract(OnboardingType.INSTITUTION, ContractType.ATTACHMENT,
        org.openapi.quarkus.product_json.model.InstitutionType.PA, path);
    config.setOrder(order);
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






