package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;


import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import it.pagopa.selfcare.party.registry_proxy.web.model.GeographicTaxonomyResource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeographicTaxonomyMapper {
    public static GeographicTaxonomyResource toResource(GeographicTaxonomy geographicTaxonomy) {
        log.trace("toResource start");
        log.debug("toResource entity = {}", geographicTaxonomy);
        GeographicTaxonomyResource geographicTaxonomyResource = null;
        if (geographicTaxonomy != null) {
            geographicTaxonomyResource = new GeographicTaxonomyResource();
            geographicTaxonomyResource.setDescription(geographicTaxonomy.getDescription());
            geographicTaxonomyResource.setGeotaxId(geographicTaxonomy.getGeotaxId());
            geographicTaxonomyResource.setEnabled(geographicTaxonomy.isEnabled());
            geographicTaxonomyResource.setRegionId(geographicTaxonomy.getRegionId());
            geographicTaxonomyResource.setProvinceId(geographicTaxonomy.getProvinceId());
            geographicTaxonomyResource.setProvinceAbbreviation(geographicTaxonomy.getProvinceAbbreviation());
            geographicTaxonomyResource.setCountry(geographicTaxonomy.getCountry());
            geographicTaxonomyResource.setCountryAbbreviation(geographicTaxonomy.getCountryAbbreviation());
            geographicTaxonomyResource.setIstatCode(geographicTaxonomy.getIstatCode());
        }
        log.debug("toResource result = {}", geographicTaxonomyResource);
        log.trace("toResource end");

        return geographicTaxonomyResource;
    }
}

