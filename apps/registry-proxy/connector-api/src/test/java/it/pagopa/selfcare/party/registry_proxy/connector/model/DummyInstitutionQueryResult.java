package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class DummyInstitutionQueryResult implements QueryResult<Institution> {

    private List<Institution> items = List.of();
    private long totalHits;

}