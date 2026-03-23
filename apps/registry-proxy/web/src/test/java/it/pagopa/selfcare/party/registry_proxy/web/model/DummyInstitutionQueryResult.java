package it.pagopa.selfcare.party.registry_proxy.web.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.Data;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;

@Data
public class DummyInstitutionQueryResult implements QueryResult<Institution> {

    private List<Institution> items = List.of(mockInstance(new DummyInstitution()));
    private long totalHits;

}