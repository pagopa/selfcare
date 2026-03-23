package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class DummyCategoryQueryResult implements QueryResult<Category> {

    private List<Category> items = List.of();
    private long totalHits;

}