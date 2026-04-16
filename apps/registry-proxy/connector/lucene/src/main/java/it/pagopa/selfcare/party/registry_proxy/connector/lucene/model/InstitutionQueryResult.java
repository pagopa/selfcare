package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionQueryResult implements QueryResult<Institution> {

    private List<Institution> items;
    private long totalHits;

}
