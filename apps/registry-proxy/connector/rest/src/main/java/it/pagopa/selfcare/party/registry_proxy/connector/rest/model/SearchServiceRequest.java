package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class SearchServiceRequest {
  List<SearchServiceInstitutionRequest> value;

  public static SearchServiceRequest createFromInstitution(SearchServiceInstitutionRequest searchServiceInstitutionRequest) {
    SearchServiceRequest searchServiceRequest = new SearchServiceRequest();
    searchServiceRequest.setValue(new ArrayList<>());
    searchServiceRequest.getValue().add(searchServiceInstitutionRequest);
    return searchServiceRequest;
  }
}
