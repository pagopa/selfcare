package it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LegalAddressFilter {

    @JsonProperty("taxId")
    private String taxId;

}
