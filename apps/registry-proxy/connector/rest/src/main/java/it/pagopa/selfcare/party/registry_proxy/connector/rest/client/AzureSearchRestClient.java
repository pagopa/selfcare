package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.AzureSearchRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceRequestBody;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "${rest-client.ai-search.serviceCode}", url = "${rest-client.ai-search.base-url}", configuration = AzureSearchRestClientConfig.class)
public interface AzureSearchRestClient {
  @PostMapping(value = "${rest-client.ai-search.institution.add.path}", consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  SearchServiceStatus indexInstitution(@RequestBody SearchServiceRequest searchServiceRequest);

  @GetMapping("${rest-client.ai-search.institution.search.path}")
  SearchServiceResponse searchInstitution(
    @RequestParam("search") String search,
    @RequestParam(value = "$filter", required = false) String filter,
    @RequestParam(value = "$top", required = false) Integer top,
    @RequestParam(value = "$skip", required = false) Integer skip,
    @RequestParam(value = "$select", required = false) String select,
    @RequestParam(value = "$orderby", required = false) String orderby
  );

  @PostMapping("/indexes/{indexName}/docs/search")
  SearchServiceResponse searchWithBody(
    @PathVariable("indexName") String indexName,
    @RequestParam("api-version") String apiVersion,
    @RequestBody SearchServiceRequestBody searchRequest
  );
}
