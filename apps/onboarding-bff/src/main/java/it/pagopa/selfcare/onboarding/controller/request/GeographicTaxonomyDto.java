package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class GeographicTaxonomyDto {


    @Schema(description = "${openapi.onboarding.geographicTaxonomy.model.code}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String code;

    @Schema(description = "${openapi.onboarding.geographicTaxonomy.model.desc}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String desc;
}
