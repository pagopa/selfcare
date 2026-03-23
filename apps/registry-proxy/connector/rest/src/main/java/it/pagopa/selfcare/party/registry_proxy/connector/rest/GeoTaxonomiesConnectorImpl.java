package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.GeoTaxonomiesRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy.GeographicTaxonomyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class GeoTaxonomiesConnectorImpl implements GeoTaxonomiesConnector {

    public static final String CODE_S_NOT_FOUND = "Code %s not found!";
    public static final String ERROR_GEO_TAXONOMIES_REST_CLIENT_MESSAGE = "Error geo-taxonomies rest client, message: %s";
    private final GeoTaxonomiesRestClient restClient;
    @Autowired
    public GeoTaxonomiesConnectorImpl(GeoTaxonomiesRestClient restClient) {
        this.restClient = restClient;
    }


    @Override
    @CircuitBreaker(name = "geotaxCircuitbreaker", fallbackMethod = "fallbackGetExtByDescription")
    @Retry(name = "retryServiceUnavailable")
    public List<GeographicTaxonomy> getExtByDescription(String description, Integer offset, Integer limit) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getExtByDescription description = {}", description);
        Assert.hasText(description, "Description is required");

        GeographicTaxonomiesResponse result = restClient.getExtByDescription(description, offset, limit);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getExtByDescription result = {}", result);
        return toGeoTaxonomiesList(result.getGeographicTaxonomiesResponse());
    }

    public List<GeographicTaxonomy> fallbackGetExtByDescription(RuntimeException e) {
        log.error(String.format(ERROR_GEO_TAXONOMIES_REST_CLIENT_MESSAGE, e.getMessage()));
        return List.of();
    }

    @Override
    @CircuitBreaker(name = "geotaxCircuitbreaker", fallbackMethod = "fallbackGetExtByCode")
    @Retry(name = "retryServiceUnavailable")
    public GeographicTaxonomy getExtByCode(String code) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getExtByCode code = {}", code);
        Assert.hasText(code, "Code is required");

        GeographicTaxonomyResponse result = restClient.getExtByCode(code);
        if(Objects.isNull(result)) throw new ResourceNotFoundException(String.format(CODE_S_NOT_FOUND, code));

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getExtByCode result = {}", result);
        return toGeoTaxonomy(result);
    }

    public GeographicTaxonomy fallbackGetExtByCode(RuntimeException e) {
        log.error(String.format(ERROR_GEO_TAXONOMIES_REST_CLIENT_MESSAGE, e.getMessage()));
        throw new ResourceNotFoundException("");
    }

    private GeographicTaxonomy toGeoTaxonomy(GeographicTaxonomyResponse geographicTaxonomyResponse) {
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setDescription(geographicTaxonomyResponse.getDescription());
        geographicTaxonomy.setGeotaxId(geographicTaxonomyResponse.getGeotaxId());
        geographicTaxonomy.setEnabled(geographicTaxonomyResponse.isEnabled());
        geographicTaxonomy.setRegionId(geographicTaxonomyResponse.getRegionId());
        geographicTaxonomy.setProvinceId(geographicTaxonomyResponse.getProvinceId());
        geographicTaxonomy.setProvinceAbbreviation(geographicTaxonomyResponse.getProvinceAbbreviation());
        geographicTaxonomy.setCountry(geographicTaxonomyResponse.getCountry());
        geographicTaxonomy.setCountryAbbreviation(geographicTaxonomyResponse.getCountryAbbreviation());
        geographicTaxonomy.setIstatCode(geographicTaxonomyResponse.getIstatCode());
        return geographicTaxonomy;
    }


    private List<GeographicTaxonomy> toGeoTaxonomiesList(List<GeographicTaxonomyResponse> result) {
        List<GeographicTaxonomy> geographicTaxonomyList = new ArrayList<>();
        for(GeographicTaxonomyResponse geoTaxonomiesResult : result) {
            GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
            geographicTaxonomy.setDescription(geoTaxonomiesResult.getDescription());
            geographicTaxonomy.setGeotaxId(geoTaxonomiesResult.getGeotaxId());
            geographicTaxonomy.setEnabled(geoTaxonomiesResult.isEnabled());
            geographicTaxonomy.setRegionId(geoTaxonomiesResult.getRegionId());
            geographicTaxonomy.setProvinceId(geoTaxonomiesResult.getProvinceId());
            geographicTaxonomy.setProvinceAbbreviation(geoTaxonomiesResult.getProvinceAbbreviation());
            geographicTaxonomy.setCountry(geoTaxonomiesResult.getCountry());
            geographicTaxonomy.setCountryAbbreviation(geoTaxonomiesResult.getCountryAbbreviation());
            geographicTaxonomy.setIstatCode(geoTaxonomiesResult.getIstatCode());
            geographicTaxonomyList.add(geographicTaxonomy);
        }
        return geographicTaxonomyList;
    }
}
