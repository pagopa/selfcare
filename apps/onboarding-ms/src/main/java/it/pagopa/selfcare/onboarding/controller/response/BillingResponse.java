package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

@Data
public class BillingResponse {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
