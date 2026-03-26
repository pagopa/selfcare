package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AssistanceContactsResource {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.supportEmail}")
    private String supportEmail;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
