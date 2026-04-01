package it.pagopa.selfcare.onboarding.connector.model.registry_proxy;

import lombok.Data;

@Data
public class GeographicTaxonomies {
    private String geotaxId;
    private String description;
    private String istatCode;
    private String provinceId;
    private String provinceAbbreviation;
    private String regionId;
    private String country;
    private String countryAbbreviation;
    private boolean enable; //REQUIRED
}
