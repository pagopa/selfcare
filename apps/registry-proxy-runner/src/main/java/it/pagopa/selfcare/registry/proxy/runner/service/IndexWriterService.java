package it.pagopa.selfcare.registry.proxy.runner.service;

import java.util.List;

/**
 * Generic interface for writing entities to an AI Search index.
 *
 * @param <T> the source entity type (from IPA open data)
 */
public interface IndexWriterService<T> {

  /**
   * Indexes a list of entities into the corresponding AI Search index, filtering only those that
   * need updating.
   */
  void index(List<T> items);
}
