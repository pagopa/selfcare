package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
public class ProductRoleMappingsResource {

    @Schema(description = "${swagger.dashboard.product-role-mappings.model.partyRole}")
    private String partyRole;

    @Schema(description = "${swagger.dashboard.product-role-mappings.model.selcRole}")
    private SelfCareAuthority selcRole;

    @Schema(description = "${swagger.dashboard.product-role-mappings.model.multiroleAllowed}")
    private boolean multiroleAllowed;

    @Schema(description = "${swagger.dashboard.product-role-mappings.model.phasesAdditionAllowed}")
    private List<String> phasesAdditionAllowed;

    @Schema(description = "${swagger.dashboard.product-role-mappings.model.productRoles}")
    private List<ProductRoleResource> productRoles;


    @Data
    @EqualsAndHashCode(of = "code")
    public static class ProductRoleResource {

        @Schema(description = "${swagger.dashboard.product-role.model.code}")
        private String code;

        @Schema(description = "${swagger.dashboard.product-role.model.label}")
        private String label;

        @Schema(description = "${swagger.dashboard.product-role.model.description}")
        private String description;
        
    }

}
