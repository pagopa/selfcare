package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;

import java.util.Optional;

public interface IvassService {
    QueryResult<InsuranceCompany> search(Optional<String> searchText, int page, int limit);
    InsuranceCompany findByOriginId(String ivassCode);
}

