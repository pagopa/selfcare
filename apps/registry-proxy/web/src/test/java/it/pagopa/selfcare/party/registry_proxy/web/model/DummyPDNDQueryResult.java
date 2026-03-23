package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;

@Data
public class DummyPDNDQueryResult implements QueryResult<Station> {

    private List<Station> items = List.of(mockInstance(new DummyPDND()));
    private long totalHits;

}