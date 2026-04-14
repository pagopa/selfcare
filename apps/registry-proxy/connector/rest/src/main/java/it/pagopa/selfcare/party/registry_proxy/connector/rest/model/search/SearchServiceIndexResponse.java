package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchServiceIndexResponse<T extends SearchServiceIndex> {

    @JsonProperty("@odata.context")
    private String context;

    @JsonProperty("@odata.count")
    private Long count;

    private List<T> value;

}
