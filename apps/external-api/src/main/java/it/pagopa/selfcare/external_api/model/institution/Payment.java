package it.pagopa.selfcare.external_api.model.institution;

import lombok.Data;

@Data
public class Payment {
    private String iban;
    private String holder;
}
