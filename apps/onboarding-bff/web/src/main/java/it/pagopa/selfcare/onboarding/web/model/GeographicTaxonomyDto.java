package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class GeographicTaxonomyDto {


    @ApiModelProperty(value = "${swagger.onboarding.geographicTaxonomy.model.code}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String code;

    @ApiModelProperty(value = "${swagger.onboarding.geographicTaxonomy.model.desc}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String desc;
}
