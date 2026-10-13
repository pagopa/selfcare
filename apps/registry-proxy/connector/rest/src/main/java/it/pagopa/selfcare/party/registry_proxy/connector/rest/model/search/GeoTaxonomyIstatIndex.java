package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoTaxonomyIstatIndex extends SearchServiceIndex {

    private String id;
    private String geotaxId;
    private String istatCode;
    private String description;
    private String provinceId;
    private String provinceAbbreviation;
    private String regionId;
    private String country;
    private String countryAbbreviation;
    private Boolean enabled;
    private String updateDate;
}

