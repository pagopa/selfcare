package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionLocationDataDto {
    @Schema(description = "${swagger.external_api.institutions.model.city}")
    private String city;

    @Schema(description = "${swagger.external_api.institutions.model.county}")
    private String county;

    @Schema(description = "${swagger.external_api.institutions.model.country}")
    private String country;

}

