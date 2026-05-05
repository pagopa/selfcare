package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;

public interface IndexEntityMapper<T, I extends SearchServiceIndex> {
  T toEntity(I index);

  SearchResult<T> toSearchResult(SearchServiceIndexResponse<I> response);
}
