package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import lombok.Data;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;

@Data
public class DummyUOQueryResult implements QueryResult<UO> {

    private List<UO> items = List.of(mockInstance(new DummyUO()));
    private long totalHits;

}