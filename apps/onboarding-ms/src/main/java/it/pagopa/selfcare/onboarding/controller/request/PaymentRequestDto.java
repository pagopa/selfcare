package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @Pattern(regexp = "^IT\\d{2}[A-Z]\\d{5}\\d{5}[0-9A-Z]{12}$",
            message = "IBAN is not in an Italian format or is not 27 characters long")
    private String iban;

    private String holder;
}
