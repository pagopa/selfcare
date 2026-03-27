package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InstitutionLegalAddressResource {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.address}")
    private String address;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

}
