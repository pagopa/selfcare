package it.pagopa.selfcare.onboarding.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class GeographicTaxonomyFromIstatCode {

    private String city;
    private String county;
}
