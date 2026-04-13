package it.pagopa.selfcare.onboarding.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ReasonForRejectDto {

    @ApiModelProperty(value = "${swagger.onboarding.institution.model.reason}")
    private String reason;
}
