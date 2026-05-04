package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for AI Search index writers. Provides common batch indexing logic and
 * update-date comparison.
 *
 * @param <T> the source entity type (from IPA open data)
 * @param <D> the index document type (for AI Search)
 */
@Slf4j
public abstract class AbstractIndexWriterService<T, D> implements IndexWriterService<T> {

  private static final int BATCH_SIZE = 1000;

  private final AzureSearchRestClient azureSearchRestClient;

  protected AbstractIndexWriterService(AzureSearchRestClient azureSearchRestClient) {
    this.azureSearchRestClient = azureSearchRestClient;
  }

  @Override
  public void index(List<T> items) {
    if (items.isEmpty()) {
      log.warn("[{}] No items to index, skipping", getEntityName());
      return;
    }

    Map<String, String> itemsIndexed = fetchAll();

    List<T> toIndex =
        items.stream()
            .filter(item -> getId(item) != null && !getId(item).isBlank())
            .filter(item -> needsUpdate(itemsIndexed.get(getId(item)), item))
            .toList();

    if (toIndex.isEmpty()) {
      log.info("[{}] AI Search index is already up to date", getEntityName());
      return;
    }

    log.info("[{}] Indexing {} items (new or updated)", getEntityName(), toIndex.size());
    for (int i = 0; i < toIndex.size(); i += BATCH_SIZE) {
      List<D> batch =
          toIndex.subList(i, Math.min(i + BATCH_SIZE, toIndex.size())).stream()
              .map(toDocument())
              .toList();
      SearchServiceIndexRequest request = new SearchServiceIndexRequest();
      request.setValue(batch);
      azureSearchRestClient.index(getIndexName(), getApiVersion(), request);
      log.debug("[{}] Indexed batch of {} documents", getEntityName(), batch.size());
    }

    // TODO: consider deleting documents that are no longer present in the source data (not in
    // itemsIndexed)
  }

  /**
   * Determines whether the item needs to be re-indexed by comparing the updateDate from the CSV
   * with the one stored in AI Search.
   */
  protected boolean needsUpdate(String currentUpdateDate, T item) {
    // String currentUpdateDate = fetchUpdateDate(getId(item));
    if (currentUpdateDate == null) {
      return true;
    }
    String csvUpdateDate = getUpdateDate(item);
    return csvUpdateDate == null
        || csvUpdateDate.isBlank()
        || !currentUpdateDate.equals(csvUpdateDate);
  }

  private String fetchUpdateDate(String id) {
    String filter = "id eq '" + id + "'";
    try {
      SearchServiceIndexResponse response =
          azureSearchRestClient.search(
              getIndexName(), getApiVersion(), "*", null, false, 1, 0, filter);

      if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
        return null;
      }
      return response.getValue().get(0).getUpdateDate();
    } catch (Exception e) {
      log.error("[{}] Error fetching '{}' from AI Search index", getEntityName(), id, e);
      return null;
    }
  }

  private Map<String, String> fetchAll() {
    Map<String, String> result = new HashMap<>();
    int skip = 0;
    while (true) {
      try {
        SearchServiceIndexResponse response =
            azureSearchRestClient.search(
                getIndexName(),
                getApiVersion(),
                "*",
                "id,updateDate",
                true,
                BATCH_SIZE,
                skip,
                null);

        if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
          break;
        }

        response
            .getValue()
            .forEach(
                doc -> {
                  if (doc.getId() != null) {
                    result.put(doc.getId(), doc.getUpdateDate());
                  }
                });
        if (response.getValue().size() < BATCH_SIZE) {
          break;
        }
      } catch (Exception e) {
        log.error("[{}] Error fetching from AI Search index", getEntityName(), e);
        break;
      }

      skip += BATCH_SIZE;
    }
    log.debug("Fetched {} IPA institution documents from AI Search index", result.size());
    return result;
  }

  /** Returns the unique ID of the entity. */
  protected abstract String getId(T item);

  /** Returns the update date of the entity (used for change detection). */
  protected abstract String getUpdateDate(T item);

  /** Maps the source entity to the AI Search index document. */
  protected abstract Function<T, D> toDocument();

  /** Returns the AI Search index name. */
  protected abstract String getIndexName();

  /** Returns the AI Search API version. */
  protected abstract String getApiVersion();

  /** Returns a human-readable entity name for logging. */
  protected abstract String getEntityName();
}
