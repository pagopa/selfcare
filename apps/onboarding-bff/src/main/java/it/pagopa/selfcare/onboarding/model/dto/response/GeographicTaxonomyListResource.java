package it.pagopa.selfcare.onboarding.model.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class GeographicTaxonomyListResource {
    @Schema(description = "${openapi.onboarding.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;
}