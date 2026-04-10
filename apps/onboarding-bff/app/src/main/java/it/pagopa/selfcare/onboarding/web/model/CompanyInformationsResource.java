package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CompanyInformationsResource {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.companyInformations.rea}")
    private String rea;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.companyInformations.shareCapital}")
    private String shareCapital;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.companyInformations.businessRegisterPlace}")
    private String businessRegisterPlace;

}
