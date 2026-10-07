package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.external_api.model.product.ProductRoleInfo;
import it.pagopa.selfcare.external_api.utils.ProductMsUtils;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import org.mapstruct.Mapper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface ProductsMapper {

  default ProductResource toResource(ProductResponse model, String institutionType) {
    if (model == null) {
      return null;
    }
    ProductResource resource = new ProductResource();
    resource.setId(model.getProductId());
    resource.setTitle(model.getTitle());
    resource.setDescription(model.getDescription());
    resource.setParentId(model.getParentId());
    if (model.getVisualConfiguration() != null) {
      resource.setLogo(model.getVisualConfiguration().getLogoUrl());
      resource.setDepictImageUrl(model.getVisualConfiguration().getDepictImageUrl());
      resource.setLogoBgColor(model.getVisualConfiguration().getLogoBgColor());
    }
    if (model.getMetadata() != null && model.getMetadata().getCreatedAt() != null) {
      resource.setCreatedAt(model.getMetadata().getCreatedAt().toInstant());
    }
    ProductMsUtils.getProdBackOfficeConfiguration(model).ifPresent(configuration -> {
      resource.setUrlPublic(configuration.getUrlPublic());
      resource.setUrlBO(configuration.getUrlBO());
      resource.setIdentityTokenAudience(configuration.getIdentityTokenAudience());
    });
    ProductMsUtils.getInstitutionContract(model, institutionType).ifPresent(contract -> {
      resource.setContractTemplatePath(contract.getPath());
      resource.setContractTemplateVersion(contract.getVersion());
    });
    resource.setRoleMappings(toRoleMappings(ProductMsUtils.getRoleMappings(model, institutionType)));
    return resource;
  }

  it.pagopa.selfcare.external_api.model.product.ProductRoleInfo.ProductRole toProductRole(BackOfficeRole productRole);

  default EnumMap<PartyRole, ProductRoleInfo> toRoleMappings(Map<PartyRole, List<RoleMapping>> roleMappings) {
    if (roleMappings == null) {
      return null;
    }
    EnumMap<PartyRole, ProductRoleInfo> result = new EnumMap<>(PartyRole.class);
    roleMappings.forEach((partyRole, mappings) -> {
      ProductRoleInfo productRoleInfo = new ProductRoleInfo();
      List<BackOfficeRole> roles = mappings.stream()
              .filter(mapping -> mapping.getBackOfficeRoles() != null)
              .flatMap(mapping -> mapping.getBackOfficeRoles().stream())
              .filter(Objects::nonNull)
              .toList();
      productRoleInfo.setRoles(roles.stream().map(this::toProductRole).toList());
      RoleMapping firstMapping = mappings.get(0);
      productRoleInfo.setSkipUserCreation(Boolean.TRUE.equals(firstMapping.getSkipUserCreation()));
      productRoleInfo.setPhasesAdditionAllowed(firstMapping.getPhasesAdditionAllowed());
      productRoleInfo.setMultiroleAllowed(roles.stream()
              .map(BackOfficeRole::getMultiroleGroups)
              .filter(Objects::nonNull)
              .anyMatch(groups -> !groups.isEmpty()));
      result.put(partyRole, productRoleInfo);
    });
    return result;
  }
}
