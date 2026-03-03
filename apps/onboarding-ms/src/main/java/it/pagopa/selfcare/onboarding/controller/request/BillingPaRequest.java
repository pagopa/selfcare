package it.pagopa.selfcare.onboarding.controller.request;

import lombok.Data;

@Data
public class BillingPaRequest {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
    private String taxCodeInvoicing;
}
