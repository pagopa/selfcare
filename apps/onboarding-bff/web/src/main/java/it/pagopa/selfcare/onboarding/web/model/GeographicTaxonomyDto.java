package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class GeographicTaxonomyDto {


    @Schema(description = "${swagger.onboarding.geographicTaxonomy.model.code}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String code;

    @Schema(description = "${swagger.onboarding.geographicTaxonomy.model.desc}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String desc;
}
