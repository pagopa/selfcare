package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
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
