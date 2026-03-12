package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.Origin;
import lombok.Data;

import java.util.List;

@Data
@SuppressWarnings("java:S1068")
public class AggregateInstitution {

    private String taxCode;
    private String description;
    private String subunitCode;
    private String subunitType;
    private String parentDescription;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;
    private String vatNumber;
    private List<User> users;
    private String recipientCode;
    private String digitalAddress;
    private String city;
    private String county;
    private String iban;
    private String istatCode;

}
