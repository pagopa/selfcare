package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.SearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper.SearchServiceMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceOnboardingIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceConnectorImpl implements SearchServiceConnector {

  private static final String SEARCH_MODE_ALL = "all";
  private static final int SEARCH_MIN_TOKEN_LENGTH = 3;

  private final AzureSearchRestClient azureSearchRestClient;
  private final SearchServiceMapper searchServiceMapper;

  public SearchServiceConnectorImpl(AzureSearchRestClient azureSearchRestClient, SearchServiceMapper searchServiceMapper) {
    this.azureSearchRestClient = azureSearchRestClient;
    this.searchServiceMapper = searchServiceMapper;
  }

  @Override
  public SearchServiceStatus indexOnboarding(OnboardingIndex onboardingIndex) {
    final SearchServiceIndexRequest<SearchServiceOnboardingIndex> searchServiceIndexRequest = new SearchServiceIndexRequest<>();
    searchServiceIndexRequest.setValue(List.of(searchServiceMapper.toSearchServiceOnboardingIndex(onboardingIndex)));
    return azureSearchRestClient.indexOnboarding(searchServiceIndexRequest);
  }

  @Override
  public OnboardingIndexSearch searchOnboarding(String search, String filter, Long top, Long skip, String orderBy) {
    final SearchServiceIndexResponse<SearchServiceOnboardingIndex> response = azureSearchRestClient.searchOnboarding(optimizeSearchString(search), SEARCH_MODE_ALL, filter, true, top, skip, null, orderBy);
    return searchServiceMapper.toOnboardingIndexSearch(response);
  }

  String optimizeSearchString(String search) {
    return Optional.ofNullable(search).map(s ->
            Arrays.stream(s.trim().replace(".", "").split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() >= SEARCH_MIN_TOKEN_LENGTH)
                .map(token -> "\\\"" + token + "\\\"")
                .collect(Collectors.joining(" ")))
        .orElse(search);
  }

}
