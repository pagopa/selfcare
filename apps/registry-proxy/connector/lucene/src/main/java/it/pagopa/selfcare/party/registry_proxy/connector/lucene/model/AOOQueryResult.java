package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

@Data
public class AOOQueryResult implements QueryResult<AOO> {

    private List<AOO> items;
    private long totalHits;

}
