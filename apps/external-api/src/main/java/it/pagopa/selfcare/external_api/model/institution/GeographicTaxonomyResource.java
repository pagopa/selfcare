package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {
    @Schema(description = "${swagger.external_api.geographicTaxonomy.model.code}")
    private String code;

    @Schema(description = "${swagger.external_api.geographicTaxonomy.model.desc}")
    private String desc;
}
