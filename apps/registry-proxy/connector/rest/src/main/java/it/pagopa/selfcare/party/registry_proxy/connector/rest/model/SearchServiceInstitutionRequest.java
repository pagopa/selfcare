package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Onboarding;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;

@Setter
@Getter
public class SearchServiceInstitutionRequest extends SearchServiceInstitution {
  @JsonProperty("@search.action")
  String action;

  public static SearchServiceInstitutionRequest createFromInstitution(Institution institution) {
    SearchServiceInstitutionRequest searchServiceInstitutionRequest = new SearchServiceInstitutionRequest();
    searchServiceInstitutionRequest.setId(institution.getId());
    searchServiceInstitutionRequest.setDescription(institution.getDescription());
    searchServiceInstitutionRequest.setParentDescription(institution.getParentDescription());
    searchServiceInstitutionRequest.setTaxCode(institution.getTaxCode());
    searchServiceInstitutionRequest.setProducts(institution.getOnboarding().stream().map(Onboarding::getProductId).toList());
    searchServiceInstitutionRequest.setInstitutionTypes(institution.getOnboarding().stream().map(Onboarding::getInstitutionType).collect(Collectors.toSet()).stream().toList());
    searchServiceInstitutionRequest.setLastModified(institution.getUpdatedAt());
    searchServiceInstitutionRequest.setAction("mergeOrUpload");
    return searchServiceInstitutionRequest;
  }
}
