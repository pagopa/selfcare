package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingInfoResource {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.institutionData}")
    private InstitutionData institution;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;

}
