package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.dto.AttachmentTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
import org.openapi.quarkus.product_json.model.EmailTemplateConfig;
import org.openapi.quarkus.product_json.model.OnboardingType;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;

public final class ProductConfigUtils {

  public static final int DEFAULT_EXPIRATION_DAYS = 30;
  public static final String DEFAULT_INSTITUTION_TYPE = "DEFAULT";

  private ProductConfigUtils() {}

  public static Map<PartyRole, RoleMapping> roleMappings(ProductResponse product, String institutionType) {
    List<RoleMapping> mappings = Optional.ofNullable(product)
        .map(ProductResponse::getRoleMappings)
        .orElseThrow(() -> new IllegalStateException("Role mappings are missing for product " + productId(product)));
    List<RoleMapping> selected = mappings.stream()
        .filter(mapping -> matchesInstitutionType(mapping.getInstitutionType(), institutionType))
        .toList();
    if (selected.isEmpty()) {
      selected = mappings.stream()
          .filter(mapping -> isDefault(mapping.getInstitutionType()))
          .toList();
    }
    if (selected.isEmpty()) {
      throw new IllegalStateException("Role mappings are missing for product " + productId(product)
          + " and institution type " + institutionType);
    }
    return selected.stream().filter(mapping -> mapping.getRole() != null)
        .collect(Collectors.toMap(mapping -> PartyRole.valueOf(mapping.getRole()), Function.identity(), (first, ignored) -> first));
  }

  public static Optional<ContractTemplateConfig> contractTemplate(
      ProductResponse product, OnboardingType onboardingType, String institutionType) {
    List<ContractTemplateConfig> contracts = contracts(product);
    return contracts.stream()
        .filter(ProductConfigUtils::isEnabled)
        .filter(config -> config.getContractType() == ContractType.CONTRACT)
        .filter(config -> config.getOnboardingType() == onboardingType)
        .filter(config -> matchesInstitutionType(config.getInstitutionType(), institutionType))
        .findFirst()
        .or(() -> contracts.stream()
            .filter(ProductConfigUtils::isEnabled)
            .filter(config -> config.getContractType() == ContractType.CONTRACT)
            .filter(config -> config.getOnboardingType() == onboardingType)
            .filter(config -> isDefault(config.getInstitutionType()))
            .findFirst());
  }

  public static List<AttachmentTemplate> attachments(
      ProductResponse product, OnboardingType onboardingType, String institutionType) {
    List<ContractTemplateConfig> contracts = contracts(product);
    boolean hasSpecificConfiguration = contracts.stream()
        .filter(ProductConfigUtils::isEnabled)
        .filter(config -> config.getOnboardingType() == onboardingType)
        .anyMatch(config -> matchesInstitutionType(config.getInstitutionType(), institutionType));
    String resolvedInstitutionType = hasSpecificConfiguration ? institutionType : DEFAULT_INSTITUTION_TYPE;

    return contracts.stream()
        .filter(ProductConfigUtils::isEnabled)
        .filter(config -> config.getContractType() == ContractType.ATTACHMENT)
        .filter(config -> config.getOnboardingType() == onboardingType)
        .filter(config -> hasSpecificConfiguration
            ? matchesInstitutionType(config.getInstitutionType(), resolvedInstitutionType)
            : isDefault(config.getInstitutionType()))
        .sorted(Comparator.comparing(config -> Optional.ofNullable(config.getOrder()).orElse(0)))
        .map(ProductConfigUtils::toAttachmentTemplate)
        .toList();
  }

  public static Optional<EmailTemplateConfig> emailTemplate(
      ProductResponse product, String institutionType, String workflowType, String status) {
    if (product == null || institutionType == null || workflowType == null || status == null) {
      return Optional.empty();
    }
    List<EmailTemplateConfig> templates = Optional.ofNullable(product.getEmailTemplates()).orElse(List.of());
    List<EmailTemplateConfig> exactWorkflow = templates.stream()
        .filter(template -> Objects.equals(enumValue(template.getInstitutionType()), institutionType))
        .filter(template -> Objects.equals(enumValue(template.getType()), workflowType))
        .toList();
    if (!exactWorkflow.isEmpty()) {
      return exactWorkflow.stream()
          .filter(template -> Objects.equals(enumValue(template.getStatus()), status))
          .findFirst();
    }
    return templates.stream()
        .filter(template -> isDefault(template.getInstitutionType()))
        .filter(template -> Objects.equals(enumValue(template.getType()), workflowType))
        .filter(template -> Objects.equals(enumValue(template.getStatus()), status))
        .findFirst();
  }

  public static int expirationDays(ProductResponse product) {
    return product != null && product.getFeatures() != null && product.getFeatures().getExpirationDays() != null
        ? product.getFeatures().getExpirationDays()
        : DEFAULT_EXPIRATION_DAYS;
  }

  private static AttachmentTemplate toAttachmentTemplate(ContractTemplateConfig config) {
    AttachmentTemplate attachment = new AttachmentTemplate();
    attachment.setTemplatePath(config.getPath());
    attachment.setTemplateVersion(config.getVersion());
    attachment.setName(config.getName());
    attachment.setMandatory(Boolean.TRUE.equals(config.getMandatory()));
    attachment.setGenerated(Boolean.TRUE.equals(config.getGenerated()));
    attachment.setOrder(Optional.ofNullable(config.getOrder()).orElse(0));
    if (config.getWorkflowType() != null) {
      attachment.setWorkflowType(config.getWorkflowType().stream()
          .map(type -> it.pagopa.selfcare.onboarding.common.WorkflowType.valueOf(type.name()))
          .toList());
    }
    if (config.getWorkflowState() != null) {
      attachment.setWorkflowState(it.pagopa.selfcare.onboarding.common.OnboardingStatus.valueOf(config.getWorkflowState()));
    }
    return attachment;
  }

  private static List<ContractTemplateConfig> contracts(ProductResponse product) {
    return product == null || product.getContracts() == null ? new ArrayList<>() : product.getContracts();
  }

  private static boolean isEnabled(ContractTemplateConfig config) {
    return config != null && !Boolean.FALSE.equals(config.getEnabled());
  }

  private static boolean matchesInstitutionType(Enum<?> actual, String expected) {
    return actual != null && Objects.equals(actual.name(), expected);
  }

  private static boolean isDefault(Enum<?> institutionType) {
    return institutionType == null || DEFAULT_INSTITUTION_TYPE.equals(institutionType.name());
  }

  private static String enumValue(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private static String productId(ProductResponse product) {
    return product == null ? null : product.getProductId();
  }
}

