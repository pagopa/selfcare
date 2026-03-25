package it.pagopa.selfcare.party.registry_proxy.core;


import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
@Service
@Slf4j
public class GeographicTaxonomiesServiceImpl implements GeographicTaxonomiesService{
    private final GeoTaxonomiesConnector geoTaxonomiesConnector;

    public GeographicTaxonomiesServiceImpl(GeoTaxonomiesConnector geoTaxonomiesConnector) {
        this.geoTaxonomiesConnector = geoTaxonomiesConnector;
    }


    @Override
    public List<GeographicTaxonomy> retrieveGeoTaxonomiesByDescription(String description, Integer offset, Integer limit) {
        Assert.hasText(description, "Description is required");
        return geoTaxonomiesConnector.getExtByDescription(description, offset, limit);
    }

    @Override
    public GeographicTaxonomy retriveGeoTaxonomyByCode(String code) {
        Assert.hasText(code, "Code is required");
        return geoTaxonomiesConnector.getExtByCode(code);
    }

}
