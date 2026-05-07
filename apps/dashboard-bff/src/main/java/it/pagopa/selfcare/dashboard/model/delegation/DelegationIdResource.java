package it.pagopa.selfcare.dashboard.model.delegation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DelegationIdResource {
    @Schema(description = "${swagger.dashboard.delegation.model.id}")
    private String id;
}
