package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;

import java.util.Optional;

/**
 * Connector to the custom ISTAT geotaxonomy index used as fallback when the datalake
 * does not return a geotaxonomy (e.g. ISTAT codes already updated by IPA but not yet by ANPR).
 */
public interface GeoTaxonomiesIstatIndexConnector {

    /**
     * Looks up a geotaxonomy by code (ISTAT code) on the fallback index.
     *
     * @param code geotaxonomy code (equals the ISTAT code)
     * @return the geotaxonomy if present, {@link Optional#empty()} if not found, invalid or on index errors
     */
    Optional<GeographicTaxonomy> findByCode(String code);
}

