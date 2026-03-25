package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LegalInstutionsRequestBodyFilterDto {

    @JsonProperty("taxId")
    private String taxId;

}
