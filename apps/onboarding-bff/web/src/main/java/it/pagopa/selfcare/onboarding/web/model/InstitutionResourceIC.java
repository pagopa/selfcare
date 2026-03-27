package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResourceIC {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.legalTaxId}")
    private String legalTaxId;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.requestDateTime}")
    private String requestDateTime;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.businesses}")
    private List<BusinessResourceIC> businesses;

}
