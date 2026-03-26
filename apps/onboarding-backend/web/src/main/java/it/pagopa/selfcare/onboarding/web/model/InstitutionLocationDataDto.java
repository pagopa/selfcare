package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InstitutionLocationDataDto {
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.city}")
    private String city;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.county}")
    private String county;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.country}")
    private String country;
}
