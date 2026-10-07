package it.pagopa.selfcare.onboarding.controller.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {

    @Schema(description = "${openapi.onboarding.geographicTaxonomy.model.code}")
    private String code;

    @Schema(description = "${openapi.onboarding.geographicTaxonomy.model.desc}")
    private String desc;
}
