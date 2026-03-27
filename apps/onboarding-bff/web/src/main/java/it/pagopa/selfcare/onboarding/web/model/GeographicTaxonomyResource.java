package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {

    @ApiModelProperty(value = "${swagger.onboarding.geographicTaxonomy.model.code}")
    private String code;

    @ApiModelProperty(value = "${swagger.onboarding.geographicTaxonomy.model.desc}")
    private String desc;
}
