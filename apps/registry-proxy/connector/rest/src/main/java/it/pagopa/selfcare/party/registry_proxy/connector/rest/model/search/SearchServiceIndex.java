package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SearchServiceIndex {

    @JsonProperty("@search.action")
    private String action = "mergeOrUpload";

}
