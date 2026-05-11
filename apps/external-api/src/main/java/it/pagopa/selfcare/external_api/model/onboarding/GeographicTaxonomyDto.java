package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class GeographicTaxonomyDto {

    @Schema(description = "${swagger.external_api.geographicTaxonomy.model.code}")
    @JsonProperty(required = true)
    @NotBlank
    private String code;

    @Schema(description = "${swagger.external_api.geographicTaxonomy.model.desc}")
    @JsonProperty(required = true)
    @NotBlank
    private String desc;

}
