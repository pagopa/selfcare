package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

@Data
public class Payment {
    private String iban;
    private String holder;
}
