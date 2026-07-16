package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingInfoResource {

    @Schema(description = "${openapi.onboarding.institutions.model.institutionData}")
    private InstitutionData institution;

    @Schema(description = "${openapi.onboarding.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;

}
