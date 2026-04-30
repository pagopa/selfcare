package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceStatus;
import java.util.List;
import java.util.Map;

/**
 * Generic connector interface for Azure AI Search operations.
 *
 * @param <T> the entity type to index/search
 * @param <R> the search result type, must extend {@link SearchResult}
 */
public interface SearchConnector<T, R extends SearchResult<? extends T>> {

  /**
   * Indexes a batch of entities in Azure AI Search using the mergeOrUpload action (insert or
   * update).
   *
   * @param items batch of entities to index (max 1000 per call)
   * @return indexing status from Azure AI Search
   */
  SearchServiceStatus index(List<T> items);

  /**
   * Fetches the full map of id → updateDate for all documents currently stored in the AI Search
   * index.
   *
   * @return map keyed by document id, value is the stored updateDate string (may be null)
   */
  Map<String, String> fetchAll();

  /**
   * Fetches the updateDate for a single entity by its id.
   *
   * @param id the document id
   * @return the stored updateDate string, or null if the document is not in the index
   */
  String fetchById(String id);

  /**
   * Searches the AI Search index using a text query with optional filters.
   *
   * @param search free-text search string (use "*" for all)
   * @param filter OData filter expression (may be null)
   * @param top max number of results to return
   * @param skip number of results to skip (for pagination)
   * @return search result containing matching entities and total count
   */
  R search(String search, String filter, Integer top, Integer skip);
}
