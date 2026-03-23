package it.pagopa.selfcare.party.registry_proxy.connector.model;

import java.util.List;

public interface QueryResult<T> {//TODO: use Page instead

    List<T> getItems();

    long getTotalHits();

}
