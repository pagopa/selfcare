package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PaymentDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.iban}")
    private String iban;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.holder}")
    private String holder;

}
