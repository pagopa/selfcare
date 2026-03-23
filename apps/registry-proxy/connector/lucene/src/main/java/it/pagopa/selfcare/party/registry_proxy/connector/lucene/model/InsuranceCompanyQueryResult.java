package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

@Data
public class InsuranceCompanyQueryResult implements QueryResult<InsuranceCompany> {

    private List<InsuranceCompany> items;
    private long totalHits;

}
