package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GeographicTaxonomyResponse {
    @JsonProperty("geotax_id")
    private String geotaxId; //REQUIRED
    private String description;
    @JsonProperty("istat_code")//REQUIRED
    private String istatCode;
    @JsonProperty("province_id")
    private String provinceId;
    @JsonProperty("province_abbreviation")
    private String provinceAbbreviation;
    @JsonProperty("region_id")
    private String regionId;
    private String country;
    @JsonProperty("country_abbreviation")
    private String countryAbbreviation;

    private boolean enabled; //REQUIRED
}
