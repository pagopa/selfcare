package it.pagopa.selfcare.onboarding.event.entity;

import lombok.Data;

@Data
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
    private String taxCodeInvoicing;
}
