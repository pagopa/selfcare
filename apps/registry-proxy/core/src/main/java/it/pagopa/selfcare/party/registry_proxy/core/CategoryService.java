package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;

import java.util.Optional;

public interface CategoryService {

    QueryResult<Category> search(Optional<Origin> origin, int page, int limit);

    Category findById(String id, Origin origin);

}
