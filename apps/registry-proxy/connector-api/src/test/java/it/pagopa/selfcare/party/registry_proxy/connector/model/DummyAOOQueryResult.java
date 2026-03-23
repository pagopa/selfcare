package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class DummyAOOQueryResult implements QueryResult<AOO> {

    private List<AOO> items = List.of();
    private long totalHits;

}