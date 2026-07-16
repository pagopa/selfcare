package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {

    @Schema(description = "${openapi.onboarding.geographicTaxonomy.model.code}")
    private String code;

    @Schema(description = "${openapi.onboarding.geographicTaxonomy.model.desc}")
    private String desc;
}
