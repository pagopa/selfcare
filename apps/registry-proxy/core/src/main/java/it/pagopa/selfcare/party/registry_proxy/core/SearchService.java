package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitutionSearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface SearchService {

  List<Map<String, String>> subscribe();

  List<SearchServiceInstitution> searchInstitution(String search, Long top);

  boolean indexOnboarding(OnboardingIndex onboardingIndex);
  OnboardingIndexSearch searchOnboarding(String searchText, List<String> products, List<String> institutionTypes, List<String> statuses, OffsetDateTime createdFromDate, OffsetDateTime createdToDate, Long page, Long pageSize, List<String> orderBy, boolean includeTest);

  IpaInstitutionSearchResult searchIpaInstitutions(String searchText, String category, Integer page, Integer pageSize);
}
