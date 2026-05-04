package it.pagopa.selfcare.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateInstitutionDto {

    @Schema(description = "${swagger.dashboard.institutions.model.description}")
    private String description;

    @Schema(description = "${swagger.dashboard.institutions.model.digitalAddress}")
    private String digitalAddress;


}
