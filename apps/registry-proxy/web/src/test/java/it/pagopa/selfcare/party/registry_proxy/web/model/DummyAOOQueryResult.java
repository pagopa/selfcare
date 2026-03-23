package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;

@Data
public class DummyAOOQueryResult implements QueryResult<AOO> {

    private List<AOO> items = List.of(mockInstance(new DummyAOO()));
    private long totalHits;

}