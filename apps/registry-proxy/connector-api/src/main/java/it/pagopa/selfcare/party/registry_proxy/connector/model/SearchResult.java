package it.pagopa.selfcare.party.registry_proxy.connector.model;

import java.util.List;

public interface SearchResult<T> {
  List<T> getItems();

  long getTotalElements();
}
