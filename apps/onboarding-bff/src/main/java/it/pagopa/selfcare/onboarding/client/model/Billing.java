package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

@Data
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private Boolean publicServices;
    private String taxCodeInvoicing;
}
