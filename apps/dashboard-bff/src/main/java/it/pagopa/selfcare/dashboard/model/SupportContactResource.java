package it.pagopa.selfcare.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
@Data
public class SupportContactResource {
    @Schema(description = "${swagger.dashboard.institutions.model.supportContact.supportEmail}")
    private String supportEmail;
    @Schema(description = "${swagger.dashboard.institutions.model.supportContact.supportPhone}")
    private String supportPhone;
}
