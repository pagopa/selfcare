package it.pagopa.selfcare.onboarding.connector.rest.model;

import lombok.Data;

@Data
public class BillingResponse {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
