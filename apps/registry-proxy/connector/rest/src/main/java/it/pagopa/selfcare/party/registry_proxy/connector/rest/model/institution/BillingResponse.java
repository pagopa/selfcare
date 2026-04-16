package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.institution;

import lombok.Data;

@Data
public class BillingResponse {
    private String vatNumber;
    private String taxCodeInvoicing;
    private String recipientCode;
    private boolean publicServices;
}

