package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface ProductsMapper {

  @Mapping(
      target = "roleMappings",
      expression = "java(toRoleMappings(model.getRoleMappings(institutionType)))")
  @Mapping(
      target = "contractTemplatePath",
      expression = "java(toContractTemplatePath(model,institutionType))")
  @Mapping(
      target = "contractTemplateVersion",
      expression = "java(toContractTemplateVersion(model,institutionType))")
  ProductResource toResource(Product model, String institutionType);

  it.pagopa.selfcare.external_api.model.product.ProductRoleInfo.ProductRole toProductRole(it.pagopa.selfcare.product.entity.ProductRole productRole);

  @Named("toRoleMappings")
  default EnumMap<PartyRole, it.pagopa.selfcare.external_api.model.product.ProductRoleInfo> toRoleMappings(
      Map<PartyRole, ProductRoleInfo> roleMappings) {
    EnumMap<PartyRole, it.pagopa.selfcare.external_api.model.product.ProductRoleInfo> result;
    if (roleMappings != null) {
      result = new EnumMap<>(PartyRole.class);

      roleMappings.forEach(
          (key, value) -> {
            it.pagopa.selfcare.external_api.model.product.ProductRoleInfo productRoleInfo = new it.pagopa.selfcare.external_api.model.product.ProductRoleInfo();
            productRoleInfo.setRoles(value.getRoles().stream().map(this::toProductRole).toList());
            productRoleInfo.setSkipUserCreation(value.isSkipUserCreation());
            productRoleInfo.setPhasesAdditionAllowed(value.getPhasesAdditionAllowed());
            productRoleInfo.setMultiroleAllowed(
                    value.getRoles().stream()
                            .map(it.pagopa.selfcare.product.entity.ProductRole::getMultiroleGroups)
                            .filter(Objects::nonNull)
                            .anyMatch(group -> !group.isEmpty()));
            result.put(key, productRoleInfo);
          });
    } else {
      result = null;
    }
    return result;
  }

  @Named("toContractTemplatePath")
  default String toContractTemplatePath(Product model, String institutionType) {
    return Optional.ofNullable(
            model.getInstitutionContractTemplate(institutionType).getContractTemplatePath())
        .orElse(null);
  }

  @Named("toContractTemplateVersion")
  default String toContractTemplateVersion(Product model, String institutionType) {
    return Optional.ofNullable(
            model.getInstitutionContractTemplate(institutionType).getContractTemplateVersion())
        .orElse(null);
  }
}
