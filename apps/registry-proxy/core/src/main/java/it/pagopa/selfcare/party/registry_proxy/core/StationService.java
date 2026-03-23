package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;

import java.util.Optional;

public interface StationService {
    QueryResult<Station> search(Optional<String> searchText, int page, int limit);

    Station findByTaxId(String taxId);

}
