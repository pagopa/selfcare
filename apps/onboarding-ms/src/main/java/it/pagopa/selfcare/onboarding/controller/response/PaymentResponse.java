package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

@Data
public class PaymentResponse {
    private String iban;
    private String holder;
}
