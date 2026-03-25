package it.pagopa.selfcare.party.registry_proxy.connector.api;


import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;

import java.util.List;

public interface GeoTaxonomiesConnector {

    List<GeographicTaxonomy> getExtByDescription(String description, Integer offset, Integer limit);

    GeographicTaxonomy getExtByCode(String code);
}
