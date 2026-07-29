package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {

    @Schema(description = "${swagger.onboarding.geographicTaxonomy.model.code}")
    private String code;

    @Schema(description = "${swagger.onboarding.geographicTaxonomy.model.desc}")
    private String desc;
}
