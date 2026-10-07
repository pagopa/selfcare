package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionLocationDataDto {
    @Schema(description = "${openapi.onboarding.institutions.model.city}")
    private String city;

    @Schema(description = "${openapi.onboarding.institutions.model.county}")
    private String county;

    @Schema(description = "${openapi.onboarding.institutions.model.country}")
    private String country;
}
