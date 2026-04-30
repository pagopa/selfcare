package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IpaSearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitutionSearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper.SearchServiceMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.IpaInstitutionIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IpaSearchServiceConnectorImpl implements IpaSearchServiceConnector {

    static final int PAGE_SIZE = 1000;

    private final AzureSearchRestClient azureSearchRestClient;
    private final SearchServiceMapper searchServiceMapper;

    public IpaSearchServiceConnectorImpl(AzureSearchRestClient azureSearchRestClient, SearchServiceMapper searchServiceMapper) {
        this.azureSearchRestClient = azureSearchRestClient;
        this.searchServiceMapper = searchServiceMapper;
    }

    @Override
    public SearchServiceStatus indexInstitutions(List<Institution> institutions) {
        List<IpaInstitutionIndex> documents = institutions.stream()
                .map(IpaInstitutionIndex::fromInstitution)
                .toList();
        SearchServiceIndexRequest<IpaInstitutionIndex> request = new SearchServiceIndexRequest<>();
        request.setValue(documents);
        return azureSearchRestClient.indexIpaInstitutions(request);
    }

    /**
     * Paginates through the entire IPA institution index and returns a map of
     * id → dataAggiornamento for every stored document.
     * Pagination stops when a page returns fewer documents than PAGE_SIZE.
     */
    @Override
    public Map<String, String> fetchAllInstitutionDataAggiornamento() {
        Map<String, String> result = new HashMap<>();
        int skip = 0;
        while (true) {
            SearchServiceIndexResponse<IpaInstitutionIndex> response =
                    azureSearchRestClient.searchIpaInstitutions("*", "id,updateDate", true, PAGE_SIZE, skip, null);

            if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
                break;
            }

            response.getValue().forEach(doc -> {
                if (doc.getId() != null) {
                    result.put(doc.getId(), doc.getUpdateDate());
                }
            });

            if (response.getValue().size() < PAGE_SIZE) {
                break;
            }
            skip += PAGE_SIZE;
        }
        log.debug("Fetched {} IPA institution documents from AI Search index", result.size());
        return result;
    }

    @Override
    public IpaInstitutionSearchResult searchIpaInstitutions(String search, String filter, Integer top, Integer skip) {
        SearchServiceIndexResponse<IpaInstitutionIndex> response =
                azureSearchRestClient.searchIpaInstitutions(search, null, true, top, skip, filter);
        return searchServiceMapper.toIpaInstitutionSearchResult(response);
    }

}
