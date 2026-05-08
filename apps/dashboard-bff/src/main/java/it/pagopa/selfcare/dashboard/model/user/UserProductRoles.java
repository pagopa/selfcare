package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UserProductRoles {

    @Schema(description = "${swagger.dashboard.user.model.role}")
    String role;

    @NotEmpty
    Set<String> productRoles;

    @Schema(description = "${swagger.dashboard.user.model.toAddOnAggregates}")
    private Boolean toAddOnAggregates;
}
