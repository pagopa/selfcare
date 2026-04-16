package it.pagopa.selfcare.party.registry_proxy.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class GeographicTaxonomyResource {
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.geotaxId}")
    @JsonProperty("code")
    private String geotaxId; //REQUIRED
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.description}")
    @JsonProperty("desc")
    private String description;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.istatCode}")
    @JsonProperty("istat_code")//REQUIRED
    private String istatCode;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.provinceId}")
    @JsonProperty("province_id")
    private String provinceId;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.provinceAbbreviation}")
    @JsonProperty("province_abbreviation")
    private String provinceAbbreviation;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.regionId}")
    @JsonProperty("region_id")
    private String regionId;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.country}")
    private String country;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.countryAbbreviation}")
    @JsonProperty("country_abbreviation")
    private String countryAbbreviation;
    @ApiModelProperty(value = "${swagger.geographicTaxonomy.model.enabled}")
    private boolean enabled; //REQUIRED
}
