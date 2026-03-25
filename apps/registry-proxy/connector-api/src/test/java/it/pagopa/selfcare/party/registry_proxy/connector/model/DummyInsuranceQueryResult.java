package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class DummyInsuranceQueryResult implements QueryResult<InsuranceCompany> {

    private List<InsuranceCompany> items = List.of();
    private long totalHits;

}