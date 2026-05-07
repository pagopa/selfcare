package it.pagopa.selfcare.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {
    @Schema(description = "${swagger.dashboard.geographicTaxonomy.model.code}")
    private String code;

    @Schema(description = "${swagger.dashboard.geographicTaxonomy.model.desc}")
    private String desc;
}
