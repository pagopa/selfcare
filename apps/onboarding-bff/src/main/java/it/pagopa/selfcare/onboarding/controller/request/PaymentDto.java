package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaymentDto {

    @Schema(description = "${openapi.onboarding.institutions.model.iban}")
    private String iban;

    @Schema(description = "${openapi.onboarding.institutions.model.holder}")
    private String holder;

}
