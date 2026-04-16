package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SearchServiceResponse {
  @JsonProperty("@odata.context")
  private String odataContext;

  @JsonProperty("@odata.count")
  private Long odataCount;

  private List<SearchServiceInstitutionResponse> value;

}
