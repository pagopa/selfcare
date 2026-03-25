package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GeographicTaxonomiesResponse {
    @JsonProperty("items")
    private List<GeographicTaxonomyResponse> geographicTaxonomiesResponse;
}
