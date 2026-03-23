package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;

@Data
public class DummyInsuranceQueryResult implements QueryResult<InsuranceCompany> {

    private List<InsuranceCompany> items = List.of(mockInstance(new DummyInsuranceCompany()));
    private long totalHits;

}