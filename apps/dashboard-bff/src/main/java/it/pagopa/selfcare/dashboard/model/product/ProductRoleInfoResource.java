package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductRoleInfoResource {

    @Schema(description = "${swagger.dashboard.user.model.relationshipId}")
    private String relationshipId;
    @Schema(description = "${swagger.dashboard.user.model.productRole}")
    private String role;
    @Schema(description = "${swagger.dashboard.user.model.status}")
    private String status;
    @Schema(description = "${swagger.dashboard.user.model.selcRole}")
    private SelfCareAuthority selcRole;
    @Schema(description = "${swagger.dashboard.user.model.role}")
    private String partyRole;
    @Schema(description = "${swagger.dashboard.user.model.createdAt}")
    private LocalDateTime createdAt;
    @Schema(description = "${swagger.dashboard.user.model.updatedAt}")
    private LocalDateTime updatedAt;

}
