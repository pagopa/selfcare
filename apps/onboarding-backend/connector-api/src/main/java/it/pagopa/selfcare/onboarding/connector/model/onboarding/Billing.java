package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;

@Data
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private Boolean publicServices;
    private String taxCodeInvoicing;
}
