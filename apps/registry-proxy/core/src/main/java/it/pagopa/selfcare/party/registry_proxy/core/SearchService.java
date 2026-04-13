package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;

import java.util.List;
import java.util.Map;

public interface SearchService {

  List<Map<String, String>> subscribe();

  boolean indexInstitution(String institutionId);
  List<SearchServiceInstitution> searchInstitution(String search, List<String> products, List<String> institutionTypes, String taxCode, Integer top, Integer skip, String select, String orderby);

  boolean indexOnboarding(OnboardingIndex onboardingIndex);
  OnboardingIndexSearch searchOnboarding(String searchText, List<String> products, List<String> institutionTypes, List<String> statuses, Long page, Long pageSize, String orderBy);
}
