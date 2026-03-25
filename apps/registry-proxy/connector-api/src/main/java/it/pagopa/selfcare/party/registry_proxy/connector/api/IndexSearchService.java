package it.pagopa.selfcare.party.registry_proxy.connector.api;


import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryFilter;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchField;
import lombok.SneakyThrows;

import java.util.List;


public interface IndexSearchService<T> {

    QueryResult<T> fullTextSearch(SearchField field, String value, int page, int limit);

    QueryResult<T> fullTextSearch(SearchField field, String value, SearchField filter, String categories, int page, int limit);

    List<T> findById(SearchField field, String value);

    @SneakyThrows
    QueryResult<T> findAll(int page, int limit, String entityType, QueryFilter... filters);
}
