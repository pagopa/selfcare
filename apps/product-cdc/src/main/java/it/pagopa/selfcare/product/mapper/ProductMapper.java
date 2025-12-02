package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.BackOfficeEnvironmentConfiguration;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.RoleMapping;
import it.pagopa.selfcare.product.model.enums.OnboardingType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

  @Mapping(target = "logo", source = "visualConfiguration.logoUrl")
  @Mapping(target = "logoBgColor", source = "visualConfiguration.logoBgColor")
  @Mapping(target = "depictImageUrl", source = "visualConfiguration.depictImageUrl")

  @Mapping(target = "createdAt", source = "metadata.createdAt")
  @Mapping(target = "createdBy", source = "metadata.createdBy")
  @Mapping(target = "modifiedAt", source = "metadata.createdAt")
  @Mapping(target = "modifiedBy", source = "metadata.createdBy")

  // Flattening delle Features
  @Mapping(target = "allowCompanyOnboarding", source = "features.allowCompanyOnboarding")
  @Mapping(target = "allowIndividualOnboarding", source = "features.allowIndividualOnboarding")
  @Mapping(target = "delegable", source = "features.delegable")
  @Mapping(target = "invoiceable", source = "features.invoiceable")
  @Mapping(target = "allowedInstitutionTaxCode", source = "features.allowedInstitutionTaxCode")
  @Mapping(target = "enabled", source = "features.enabled")
  @Mapping(target = "expirationDate", source = "features.expirationDays")

  @Mapping(target = "backOfficeEnvironmentConfigurations", source = "backOfficeEnvironmentConfigurations", qualifiedByName = "mapBackOfficeConfigs")

  @Mapping(target = "roleMappings", source = "roleMappings", qualifiedByName = "mapRoleMappings")

  @Mapping(target = "institutionContractMappings", expression = "java(mapContracts(entity.getContracts(), it.pagopa.selfcare.product.model.enums.OnboardingType.INSTITUTION))")
  @Mapping(target = "userContractMappings", expression = "java(mapContracts(entity.getContracts(), it.pagopa.selfcare.product.model.enums.OnboardingType.USER))")

  @Mapping(target = "institutionAggregatorContractMappings", ignore = true)
  @Mapping(target = "userAggregatorContractMappings", ignore = true)
  it.pagopa.selfcare.product.entity.Product toResource(Product entity);

  it.pagopa.selfcare.product.entity.BackOfficeConfigurations toBackOfficeResource(BackOfficeEnvironmentConfiguration entity);

  it.pagopa.selfcare.product.entity.ProductRoleInfo toRoleResource(RoleMapping entity);

  @Mapping(target = "contractTemplatePath", source = "path")
  @Mapping(target = "contractTemplateVersion", source = "version")
  @Mapping(target = "attachments", ignore = true) // Gli attachment in ContractTemplate non ci sono nel file sorgente inviato, logica custom se serve
  it.pagopa.selfcare.product.entity.ContractTemplate toContractResource(ContractTemplate entity);

  @Named("mapBackOfficeConfigs")
  default Map<String, it.pagopa.selfcare.product.entity.BackOfficeConfigurations> mapBackOfficeConfigs(List<BackOfficeEnvironmentConfiguration> list) {
    if (list == null) return null;
    return list.stream()
      .collect(Collectors.toMap(
        BackOfficeEnvironmentConfiguration::getEnv,
        this::toBackOfficeResource
      ));
  }

  @Named("mapRoleMappings")
  default Map<PartyRole, it.pagopa.selfcare.product.entity.ProductRoleInfo> mapRoleMappings(List<RoleMapping> list) {
    if (list == null) return null;
    return list.stream()
      .collect(Collectors.toMap(
        RoleMapping::getRole, //(es. MANAGER)
        this::toRoleResource
      ));
  }
  default Map<String, it.pagopa.selfcare.product.entity.ContractTemplate> mapContracts(List<ContractTemplate> contracts, OnboardingType type) {
    if (contracts == null) return Collections.emptyMap();

    Map<String, it.pagopa.selfcare.product.entity.ContractTemplate> result = new HashMap<>();

    for (ContractTemplate contract : contracts) {
      //(INSTITUTION o USER)
      if (contract.getOnboardingType() == type) {

        //  (es. "PA", "GSP" o "default")
        String key = (contract.getInstitutionType() != null)
          ? contract.getInstitutionType().name()
          : "default";

        result.put(key, toContractResource(contract));
      }
    }
    return result;
  }
}