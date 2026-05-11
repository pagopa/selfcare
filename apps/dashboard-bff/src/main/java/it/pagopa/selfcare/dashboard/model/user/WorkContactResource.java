package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WorkContactResource {
    @Schema(description = "${swagger.dashboard.user.model.institutionalEmail}")
    private CertifiedFieldResource<String> email;
}
