package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;

@Data
public class Payment {
    private String iban;
    private String holder;
}
