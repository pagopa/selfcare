package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import lombok.Data;

import java.util.List;

@Data
public class UOQueryResult implements QueryResult<UO> {

    private List<UO> items;
    private long totalHits;

}
