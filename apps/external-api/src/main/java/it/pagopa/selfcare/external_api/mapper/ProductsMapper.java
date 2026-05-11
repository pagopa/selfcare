package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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

  @Named("toRoleMappings")
  default EnumMap<PartyRole, ProductRoleInfo> toRoleMappings(
      Map<PartyRole, ProductRoleInfo> roleMappings) {
    EnumMap<PartyRole, ProductRoleInfo> result;
    if (roleMappings != null) {
      result = new EnumMap<>(PartyRole.class);

      roleMappings.forEach(
          (key, value) -> {
            ProductRoleInfo productRoleInfo = new ProductRoleInfo();
            productRoleInfo.setRoles(roleMappings.get(key).getRoles());
            productRoleInfo.setMultiroleAllowed(roleMappings.get(key).isMultiroleAllowed());
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
