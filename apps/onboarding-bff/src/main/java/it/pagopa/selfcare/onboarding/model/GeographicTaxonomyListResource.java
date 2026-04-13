package it.pagopa.selfcare.onboarding.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class GeographicTaxonomyListResource {
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;
}