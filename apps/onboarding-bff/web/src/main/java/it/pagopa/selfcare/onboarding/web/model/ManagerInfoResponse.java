package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ManagerInfoResponse {
    @ApiModelProperty(value = "${swagger.onboarding.user.model.name}", required = true)
    private String name;
    @ApiModelProperty(value = "${swagger.onboarding.user.model.surname}", required = true)
    private String surname;
}
