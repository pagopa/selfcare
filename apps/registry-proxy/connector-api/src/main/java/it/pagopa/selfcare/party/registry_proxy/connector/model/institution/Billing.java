package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Billing {

    @JsonProperty("vatNumber")
    private String vatNumber;

    @JsonProperty("taxCodeInvoicing")
    private String taxCodeInvoicing;

    @JsonProperty("recipientCode")
    private String recipientCode;

    @JsonProperty("publicServices")
    private boolean publicServices;
}
