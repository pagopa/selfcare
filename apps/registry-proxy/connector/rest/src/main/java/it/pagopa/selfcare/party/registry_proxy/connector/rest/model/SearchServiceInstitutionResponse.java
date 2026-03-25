package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SearchServiceInstitutionResponse extends SearchServiceInstitution {
  @JsonProperty("@search.score")
  private Double searchScore;
}
