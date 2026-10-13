package it.pagopa.selfcare.party.registry_proxy.core;


import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesIstatIndexConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GeographicTaxonomiesServiceImpl implements GeographicTaxonomiesService{
    private final GeoTaxonomiesConnector geoTaxonomiesConnector;
    private final GeoTaxonomiesIstatIndexConnector geoTaxonomiesIstatIndexConnector;
    private final boolean istatFallbackEnabled;

    public GeographicTaxonomiesServiceImpl(GeoTaxonomiesConnector geoTaxonomiesConnector,
                                           GeoTaxonomiesIstatIndexConnector geoTaxonomiesIstatIndexConnector,
                                           @Value("${geotax.istat-fallback.enabled:false}") boolean istatFallbackEnabled) {
        this.geoTaxonomiesConnector = geoTaxonomiesConnector;
        this.geoTaxonomiesIstatIndexConnector = geoTaxonomiesIstatIndexConnector;
        this.istatFallbackEnabled = istatFallbackEnabled;
        log.info("Geotaxonomy ISTAT fallback index enabled: {}", istatFallbackEnabled);
    }


    @Override
    public List<GeographicTaxonomy> retrieveGeoTaxonomiesByDescription(String description, Integer offset, Integer limit) {
        Assert.hasText(description, "Description is required");
        return geoTaxonomiesConnector.getExtByDescription(description, offset, limit);
    }

    @Override
    public GeographicTaxonomy retriveGeoTaxonomyByCode(String code) {
        Assert.hasText(code, "Code is required");
        try {
            return geoTaxonomiesConnector.getExtByCode(code);
        } catch (ResourceNotFoundException | BadGatewayException datalakeError) {
            return retrieveFromIstatFallbackIndex(code, datalakeError);
        }
    }

    private GeographicTaxonomy retrieveFromIstatFallbackIndex(String code, RuntimeException datalakeError) {
        final String safeCode = Encode.forJava(code);
        final String reason = datalakeError instanceof ResourceNotFoundException ? "NOT_FOUND" : "UNAVAILABLE";

        if (!istatFallbackEnabled) {
            log.info("Geotaxonomy code {} not retrieved from datalake (reason: {}) and ISTAT fallback index is disabled, propagating datalake error",
                    safeCode, reason);
            throw datalakeError;
        }

        log.info("Geotaxonomy code {} not retrieved from datalake (reason: {}), searching on fallback ISTAT index",
                safeCode, reason);
        Optional<GeographicTaxonomy> fromIndex = geoTaxonomiesIstatIndexConnector.findByCode(code);

        if (fromIndex.isPresent()) {
            log.info("Geotaxonomy code {} found on fallback ISTAT index (description: {}), returning it in place of datalake data",
                    safeCode, Encode.forJava(String.valueOf(fromIndex.get().getDescription())));
            return fromIndex.get();
        }

        log.warn("Geotaxonomy code {} not found either on datalake (reason: {}) or on fallback ISTAT index, propagating datalake error",
                safeCode, reason);
        throw datalakeError;
    }

}
