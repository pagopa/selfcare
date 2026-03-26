package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.Email;

@Data
public class AssistanceContactsDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.supportEmail}")
    @Email
    private String supportEmail;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance.supportPhone}")
    private String supportPhone;

}
