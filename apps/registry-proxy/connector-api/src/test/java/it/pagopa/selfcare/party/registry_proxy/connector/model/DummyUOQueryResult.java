package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class DummyUOQueryResult implements QueryResult<UO> {

    private List<UO> items = List.of();
    private long totalHits;

}