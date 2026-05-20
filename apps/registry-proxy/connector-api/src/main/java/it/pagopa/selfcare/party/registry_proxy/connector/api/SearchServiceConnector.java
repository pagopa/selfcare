package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;

public interface SearchServiceConnector {

  SearchServiceStatus indexOnboarding(OnboardingIndex onboardingIndex);
  OnboardingIndexSearch searchOnboarding(String search, String filter, Long top, Long skip, String orderBy);

}
