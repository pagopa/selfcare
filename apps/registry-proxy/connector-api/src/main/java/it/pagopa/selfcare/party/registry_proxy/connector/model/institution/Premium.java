package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Premium {

    @JsonProperty("status")
    private String status;

    @JsonProperty("contract")
    private String contract;

}
