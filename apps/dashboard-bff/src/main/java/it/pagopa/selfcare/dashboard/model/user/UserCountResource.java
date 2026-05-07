package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class UserCountResource {

    @Schema(description = "${swagger.dashboard.institutions.model.id}")
    private String institutionId;

    @Schema(description = "${swagger.dashboard.products.model.id}")
    private String productId;

    @Schema(description = "${swagger.dashboard.product-role-mappings.model.partyRole}")
    private List<String> roles;

    @Schema(description = "${swagger.dashboard.user.model.status}")
    private List<String> status;

    @Schema(description = "${swagger.dashboard.products.model.users}")
    private Long count;

}
