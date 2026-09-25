package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collection;

@Data
public class ProductRolesResource {

  @Schema(description = "${swagger.dashboard.product-roles.model.roleMappings}")
  private Collection<ProductRoleMappingsResource> roleMappings;

  @Schema(description = "${swagger.dashboard.product-roles.model.partnerTechRoleMappings}")
  private Collection<ProductRoleMappingsResource> partnerTechRoleMappings;

}
