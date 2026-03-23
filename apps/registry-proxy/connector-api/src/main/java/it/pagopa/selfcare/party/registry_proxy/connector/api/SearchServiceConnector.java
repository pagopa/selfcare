package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;

import java.util.List;

public interface SearchServiceConnector {
  SearchServiceStatus indexInstitution(Institution institution);
  List<SearchServiceInstitution> searchInstitution(String search, String filter, List<String> products, Integer top, Integer skip, String select, String orderby);


}
