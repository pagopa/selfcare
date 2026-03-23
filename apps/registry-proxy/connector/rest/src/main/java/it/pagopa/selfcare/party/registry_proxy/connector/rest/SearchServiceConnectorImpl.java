package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.party.registry_proxy.connector.api.SearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.model.institution.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceInstitutionRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.SearchServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class SearchServiceConnectorImpl implements SearchServiceConnector {

  private final AzureSearchRestClient azureSearchRestClient;

  public SearchServiceConnectorImpl(AzureSearchRestClient azureSearchRestClient) {
    this.azureSearchRestClient = azureSearchRestClient;
  }

  @Override
  @Retry(name = "retryServiceUnavailable")
  public SearchServiceStatus indexInstitution(Institution institution) {
    SearchServiceInstitutionRequest searchServiceInstitutionRequest = SearchServiceInstitutionRequest.createFromInstitution(institution);
    return azureSearchRestClient.indexInstitution(SearchServiceRequest.createFromInstitution(searchServiceInstitutionRequest));
  }

  @Override
  @Retry(name = "retryServiceUnavailable")
  public List<SearchServiceInstitution> searchInstitution(String search, String filter, List<String> products, Integer top, Integer skip, String select, String orderby) {
    List<String> enabledProducts = Objects.isNull(products) ? List.of() : products;
    SearchServiceResponse searchServiceResponse = azureSearchRestClient.searchInstitution(search, filter, top, skip, select, orderby);
    List<SearchServiceInstitution> institutions = new ArrayList<>();
    Optional.of(searchServiceResponse).ifPresent(response -> {
      institutions.addAll(response.getValue().stream()
        .map(SearchServiceInstitution::createSearchServiceInstitution)
        .map(searchServiceInstitution -> enabledProducts.contains("all") ? searchServiceInstitution : searchServiceInstitution.updateProductsEnable(enabledProducts))
        .toList());
    });
    return institutions;
  }
}
