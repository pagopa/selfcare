package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaymentDto {

    @Schema(description = "${swagger.onboarding.institutions.model.iban}")
    private String iban;

    @Schema(description = "${swagger.onboarding.institutions.model.holder}")
    private String holder;

}
