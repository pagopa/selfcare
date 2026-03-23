package it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LegalAddressRequest {

    @JsonProperty("filter")
    private LegalAddressFilter filter;
}
