package it.pagopa.selfcare.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeographicTaxonomyDto {
    @Schema(description = "${swagger.dashboard.geographicTaxonomy.model.code}")
    @JsonProperty(required = true)
    @NotBlank
    private String code;

    @Schema(description = "${swagger.dashboard.geographicTaxonomy.model.desc}")
    @JsonProperty(required = true)
    @NotBlank
    private String desc;
}
