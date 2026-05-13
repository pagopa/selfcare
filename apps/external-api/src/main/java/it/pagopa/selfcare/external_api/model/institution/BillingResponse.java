package it.pagopa.selfcare.external_api.model.institution;

import lombok.Data;

@Data
public class BillingResponse {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
