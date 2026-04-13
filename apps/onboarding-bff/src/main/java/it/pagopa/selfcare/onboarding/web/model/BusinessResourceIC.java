package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BusinessResourceIC {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.businessIc.model.businessName}")
    private String businessName;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.businessIc.model.businessTaxId}")
    private String businessTaxId;

}
