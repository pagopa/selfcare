package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;

import java.util.List;

public interface GeographicTaxonomiesService {

    List<GeographicTaxonomy> retrieveGeoTaxonomiesByDescription(String description, Integer offset, Integer limit);

    GeographicTaxonomy retriveGeoTaxonomyByCode(String code);

}
