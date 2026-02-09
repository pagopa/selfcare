package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.model.*;
import it.pagopa.selfcare.product.model.enums.OnboardingType;
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

  String DEFAULT_TYPE = "DEFAULT";

  @Mapping(target = "logo", source = "visualConfiguration.logoUrl")
  @Mapping(target = "logoBgColor", source = "visualConfiguration.logoBgColor")
  @Mapping(target = "depictImageUrl", source = "visualConfiguration.depictImageUrl")
  @Mapping(target = "createdAt", source = "metadata.createdAt")
  @Mapping(target = "createdBy", source = "metadata.createdBy")
  @Mapping(target = "modifiedAt", source = "metadata.createdAt")
  @Mapping(target = "modifiedBy", source = "metadata.createdBy")

  // Flattening delle Features
  @Mapping(target = "id", source = "productId")
  @Mapping(target = "allowCompanyOnboarding", source = "features.allowCompanyOnboarding")
  @Mapping(target = "allowIndividualOnboarding", source = "features.allowIndividualOnboarding")
  @Mapping(target = "delegable", source = "features.delegable")
  @Mapping(target = "invoiceable", source = "features.invoiceable")
  @Mapping(target = "allowedInstitutionTaxCode", source = "features.allowedInstitutionTaxCode")
  @Mapping(target = "enabled", source = "features.enabled")
  @Mapping(target = "expirationDate", source = "features.expirationDays")
  @Mapping(
      target = "backOfficeEnvironmentConfigurations",
      source = "backOfficeEnvironmentConfigurations",
      qualifiedByName = "mapBackOfficeConfigs")
  @Mapping(target = "roleMappings", source = "roleMappings", qualifiedByName = "mapRoleMappings")
  @Mapping(
      target = "roleMappingsByInstitutionType",
      source = "roleMappings",
      qualifiedByName = "mapRoleMappingsByInstitutionType")
  @Mapping(
      target = "institutionContractMappings",
      expression =
          "java(mapContracts(entity.getContracts(), it.pagopa.selfcare.product.model.enums.OnboardingType.INSTITUTION))")
  @Mapping(
      target = "institutionAggregatorContractMappings",
      expression =
          "java(mapContracts(entity.getContracts(), it.pagopa.selfcare.product.model.enums.OnboardingType.INSTITUTION_AGGREGATOR))")
  @Mapping(
      target = "userContractMappings",
      expression =
          "java(mapContracts(entity.getContracts(), it.pagopa.selfcare.product.model.enums.OnboardingType.USER))")
  @Mapping(
      target = "userAggregatorContractMappings",
      expression =
          "java(mapContracts(entity.getContracts(), it.pagopa.selfcare.product.model.enums.OnboardingType.USER_AGGREGATOR))")
  @Mapping(
      target = "emailTemplates",
      expression = "java(mapEmailTemplates(entity.getEmailTemplates()))")
  @Mapping(target = "status", source = "status", qualifiedByName = "mapProductStatus")
  @Mapping(
      target = "urlBO",
      expression =
          "java(mapBackOfficeConfigsProdurlBOurl(entity.getBackOfficeEnvironmentConfigurations()))")
  @Mapping(
      target = "urlPublic",
      expression =
          "java(mapBackOfficeConfigsProdurlBOurlPublic(entity.getBackOfficeEnvironmentConfigurations()))")
  @Mapping(
      target = "identityTokenAudience",
      expression =
          "java(mapBackOfficeConfigsIdentityTokenAudience(entity.getBackOfficeEnvironmentConfigurations()))")
  it.pagopa.selfcare.product.entity.Product toResource(Product entity);

  @Named("mapProductStatus")
  default ProductStatus mapProductStatus(
      it.pagopa.selfcare.product.model.enums.ProductStatus status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case ACTIVE -> ProductStatus.ACTIVE;
      case INACTIVE -> ProductStatus.INACTIVE;
      case TESTING -> ProductStatus.TESTING;
      default -> ProductStatus.PHASE_OUT;
    };
  }

  @Mapping(target = "url", source = "urlBO")
  it.pagopa.selfcare.product.entity.BackOfficeConfigurations toBackOfficeResource(
      BackOfficeEnvironmentConfiguration entity);

  @Mapping(target = "roles", source = "backOfficeRoles", qualifiedByName = "mapRoleResource")
  ProductRoleInfo toRoleResource(RoleMapping entity);

  @Named("mapRoleResource")
  it.pagopa.selfcare.product.entity.ProductRole toRole(BackOfficeRole roles);

  @Mapping(
      target = "institutionType",
      source = "institutionType",
      qualifiedByName = "mapInstitutionType")
  it.pagopa.selfcare.product.entity.OriginEntry toOriginEntryResource(OriginEntry entity);

  @Named("mapInstitutionType")
  default InstitutionType mapInstitutionType(
      it.pagopa.selfcare.product.model.enums.InstitutionType institutionType) {
    return switch (institutionType) {
      case PA -> InstitutionType.PA;
      case GSP -> InstitutionType.GSP;
      case PG -> InstitutionType.PG;
      case SA -> InstitutionType.SA;
      case PT -> InstitutionType.PT;
      case SCP -> InstitutionType.SCP;
      case PSP -> InstitutionType.PSP;
      case AS -> InstitutionType.AS;
      case REC -> InstitutionType.REC;
      case CON -> InstitutionType.CON;
      case PRV -> InstitutionType.PRV;
      case PRV_PF -> InstitutionType.PRV_PF;
      case GPU -> InstitutionType.GPU;
      case SCEC -> InstitutionType.SCEC;
      default -> null;
    };
  }

  @Named("mapOnboardingStatus")
  default OnboardingStatus mapInstitutionType(String onboardingStatus) {
    return switch (onboardingStatus) {
      case "TOBEVALIDATED" -> OnboardingStatus.TOBEVALIDATED;
      case "PENDING" -> OnboardingStatus.PENDING;
      case "COMPLETED" -> OnboardingStatus.COMPLETED;
      case "FAILED" -> OnboardingStatus.FAILED;
      case "REJECTED" -> OnboardingStatus.REJECTED;
      case "DELETED" -> OnboardingStatus.DELETED;
      default -> OnboardingStatus.REQUEST;
    };
  }

  @Named("mapWorkflowType")
  default WorkflowType mapInstitutionType(
      it.pagopa.selfcare.product.model.enums.WorkflowType workflowType) {
    return switch (workflowType) {
      case CONTRACT_REGISTRATION -> WorkflowType.CONTRACT_REGISTRATION;
      case FOR_APPROVE -> WorkflowType.FOR_APPROVE;
      case FOR_APPROVE_PT -> WorkflowType.FOR_APPROVE_PT;
      case FOR_APPROVE_GPU -> WorkflowType.FOR_APPROVE_GPU;
      case CONFIRMATION -> WorkflowType.CONFIRMATION;
      case USERS -> WorkflowType.USERS;
      case USERS_IMPORT -> WorkflowType.USERS_IMPORT;
      case IMPORT -> WorkflowType.IMPORT;
      case IMPORT_AGGREGATION -> WorkflowType.IMPORT_AGGREGATION;
      case CONTRACT_REGISTRATION_AGGREGATOR -> WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR;
      case CONFIRMATION_AGGREGATE -> WorkflowType.CONFIRMATION_AGGREGATE;
      case INCREMENT_REGISTRATION_AGGREGATOR -> WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR;
      case CONFIRMATION_AGGREGATOR -> WorkflowType.CONFIRMATION_AGGREGATOR;
      case USERS_PG -> WorkflowType.USERS_PG;
      case USERS_EA -> WorkflowType.USERS_EA;
    };
  }

  @Mapping(target = "contractTemplatePath", source = "path")
  @Mapping(target = "contractTemplateVersion", source = "version")
  @Mapping(target = "attachments", expression = "java(new ArrayList<>())")
  it.pagopa.selfcare.product.entity.ContractTemplate toContractResource(ContractTemplate entity);

  @Mapping(target = "templatePath", source = "path")
  @Mapping(target = "templateVersion", source = "version")
  @Mapping(target = "workflowType", source = "workflowType", qualifiedByName = "mapWorkflowType")
  @Mapping(
      target = "workflowState",
      source = "workflowState",
      qualifiedByName = "mapOnboardingStatus")
  it.pagopa.selfcare.product.entity.AttachmentTemplate toAttachmentResource(
      ContractTemplate entity);

  @Named("mapBackOfficeConfigsProdurlBOurl")
  default String mapBackOfficeConfigsProdurlBOurl(List<BackOfficeEnvironmentConfiguration> list) {
    if (list == null) {
      return null;
    }
    return list.stream()
        .filter(config -> config.getEnv().equalsIgnoreCase("prod"))
        .findFirst()
        .map(BackOfficeEnvironmentConfiguration::getUrlBO)
        .orElse(null);
  }

  @Named("mapBackOfficeConfigsProdurlBOurlPublic")
  default String mapBackOfficeConfigsProdurlBOurlPublic(
      List<BackOfficeEnvironmentConfiguration> list) {
    if (list == null) {
      return null;
    }
    return list.stream()
        .filter(config -> config.getEnv().equalsIgnoreCase("prod"))
        .findFirst()
        .map(BackOfficeEnvironmentConfiguration::getIdentityTokenAudience)
        .orElse(null);
  }

  @Named("mapBackOfficeConfigsIdentityTokenAudience")
  default String mapBackOfficeConfigsIdentityTokenAudience(
      List<BackOfficeEnvironmentConfiguration> list) {
    if (list == null) {
      return null;
    }
    return list.stream()
        .filter(config -> config.getEnv().equalsIgnoreCase("prod"))
        .findFirst()
        .map(BackOfficeEnvironmentConfiguration::getIdentityTokenAudience)
        .orElse(null);
  }

  @Named("mapBackOfficeConfigs")
  default Map<String, it.pagopa.selfcare.product.entity.BackOfficeConfigurations>
      mapBackOfficeConfigs(List<BackOfficeEnvironmentConfiguration> list) {
    if (list == null) {
      return null;
    }
    return list.stream()
        .collect(
            Collectors.toMap(
                BackOfficeEnvironmentConfiguration::getEnv, this::toBackOfficeResource));
  }

  @Named("mapEmailTemplates")
  default Map<String, Map<String, List<it.pagopa.selfcare.product.entity.EmailTemplate>>>
      mapEmailTemplates(List<EmailTemplate> emailTemplates) {
    if (emailTemplates == null) {
      return Collections.emptyMap();
    }

    Map<String, Map<String, List<it.pagopa.selfcare.product.entity.EmailTemplate>>> result =
        new HashMap<>();

    for (EmailTemplate template : emailTemplates) {
      String institutionType =
          template.getInstitutionType() != null
              ? template.getInstitutionType().name()
              : DEFAULT_TYPE;
      String workflowType = template.getType() != null ? template.getType().name() : DEFAULT_TYPE;

      result
          .computeIfAbsent(institutionType, k -> new HashMap<>())
          .computeIfAbsent(workflowType, k -> new ArrayList<>())
          .add(toEmailTemplateResource(template));
    }

    return result;
  }

  it.pagopa.selfcare.product.entity.EmailTemplate toEmailTemplateResource(EmailTemplate template);

  default Map<String, it.pagopa.selfcare.product.entity.ContractTemplate> mapContracts(
      List<ContractTemplate> contracts, OnboardingType type) {
    if (contracts == null) {
      return Collections.emptyMap();
    }

    Map<String, it.pagopa.selfcare.product.entity.ContractTemplate> result = new HashMap<>();

    for (ContractTemplate contract : contracts) {
      // (INSTITUTION or USER)
      if (contract.getOnboardingType() == type
          && contract.getContractType() == ContractType.CONTRACT) {
        //  (es. "PA", "GSP" o "DEFAULT")
        String key =
            (contract.getInstitutionType() != null)
                ? contract.getInstitutionType().name()
                : DEFAULT_TYPE;

        result.put(key, toContractResource(contract));
      }
    }
    for (ContractTemplate contract : contracts) {
      // (INSTITUTION or USER)
      if (contract.getOnboardingType() == type
          && contract.getContractType() == ContractType.ATTACHMENT) {
        //  (es. "PA", "GSP" o "DEFAULT")
        String key =
            (contract.getInstitutionType() != null)
                ? contract.getInstitutionType().name()
                : DEFAULT_TYPE;

        result.get(key).getAttachments().add(toAttachmentResource(contract));
      }
    }
    return result;
  }

  @Named("mapRoleMappings")
  default Map<PartyRole, it.pagopa.selfcare.product.entity.ProductRoleInfo> mapProductRole(
      List<RoleMapping> roleMappings) {
    if (roleMappings == null) {
      return null;
    }
    return roleMappings.stream()
        .filter(
            roleMapping ->
                roleMapping
                    .getInstitutionType()
                    .equals(it.pagopa.selfcare.product.model.enums.InstitutionType.DEFAULT))
        .collect(Collectors.toMap(this::getPartyRole, this::toRoleResource));
  }

  @Named("mapRoleMappingsByInstitutionType")
  default Map<String, Map<PartyRole, it.pagopa.selfcare.product.entity.ProductRoleInfo>>
      mapRoleMappingsByInstitutionType(List<RoleMapping> roleMappings) {
    if (roleMappings == null) {
      return null;
    }
    return roleMappings.stream()
        .filter(
            roleMapping ->
                !roleMapping
                    .getInstitutionType()
                    .equals(it.pagopa.selfcare.product.model.enums.InstitutionType.DEFAULT))
        .collect(
            Collectors.groupingBy(
                roleMapping ->
                    getInstitutionTypeName(mapInstitutionType(roleMapping.getInstitutionType())),
                Collectors.toMap(this::getPartyRole, this::toRoleResource)));
  }

  default String getInstitutionTypeName(InstitutionType institutionType) {
    return institutionType.name();
  }

  default PartyRole getPartyRole(RoleMapping role) {
    return switch (role.getRole()) {
      case "MANAGER" -> PartyRole.MANAGER;
      case "DELEGATE" -> PartyRole.DELEGATE;
      case "SUB_DELEGATE" -> PartyRole.SUB_DELEGATE;
      case "ADMIN_EA" -> PartyRole.ADMIN_EA;
      default -> PartyRole.OPERATOR;
    };
  }
}
