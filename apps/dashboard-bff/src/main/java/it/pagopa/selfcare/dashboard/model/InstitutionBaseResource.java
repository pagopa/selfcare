package it.pagopa.selfcare.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionBaseResource {

    @Schema(description = "${swagger.dashboard.institutions.model.id}")
    private String id;

    @Schema(description = "${swagger.dashboard.institutions.model.name}")
    private String name;

    @Schema(description = "${swagger.dashboard.model.userRole}")
    private String userRole;

    @Schema(description = "${swagger.dashboard.institutions.model.status}")
    private String status;

    @Schema(description = "${swagger.dashboard.institutions.model.parentDescription}")
    private String parentDescription;

}
