package it.pagopa.selfcare.external_api.model.onboarding;

import lombok.Data;

@Data
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private Boolean publicServices;
    private String taxCodeInvoicing;
}
