package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionLocationDataDto {
    @Schema(description = "${swagger.onboarding.institutions.model.city}")
    private String city;

    @Schema(description = "${swagger.onboarding.institutions.model.county}")
    private String county;

    @Schema(description = "${swagger.onboarding.institutions.model.country}")
    private String country;
}
