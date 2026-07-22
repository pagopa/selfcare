package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingInfoResource {

    @Schema(description = "${swagger.onboarding.institutions.model.institutionData}")
    private InstitutionData institution;

    @Schema(description = "${swagger.onboarding.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;

}
