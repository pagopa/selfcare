package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceOnboardingIndex;
import java.net.URI;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "onboarding-search", url = "https://localhost")
public interface OnboardingSearchRestClient {

  @PostMapping(value = "/indexes/{index}/docs/index?api-version=2023-11-01",
      consumes = "application/json")
  SearchServiceStatus indexOnboarding(
      URI endpoint, @PathVariable("index") String index, @RequestHeader("api-key") String apiKey,
      @RequestBody SearchServiceIndexRequest<SearchServiceOnboardingIndex> request);

  @GetMapping("/indexes/{index}/docs?api-version=2023-11-01")
  SearchServiceIndexResponse<SearchServiceOnboardingIndex> searchOnboarding(
      URI endpoint, @PathVariable("index") String index, @RequestHeader("api-key") String apiKey,
      @RequestParam(value = "search", required = false) String search,
      @RequestParam("searchMode") String searchMode,
      @RequestParam(value = "$filter", required = false) String filter,
      @RequestParam("$count") boolean count, @RequestParam("$top") Long top,
      @RequestParam("$skip") Long skip, @RequestParam("$orderby") String orderBy);
}
