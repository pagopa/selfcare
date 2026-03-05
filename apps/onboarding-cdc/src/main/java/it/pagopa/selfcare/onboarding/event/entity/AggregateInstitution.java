package it.pagopa.selfcare.onboarding.event.entity;

import it.pagopa.selfcare.onboarding.common.Origin;
import lombok.Data;

import java.util.List;

@Data
public class AggregateInstitution {

    private String taxCode;
    private String description;
    private String subunitCode;
    private String subunitType;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;
    private String vatNumber;
    private List<User> users;

}
