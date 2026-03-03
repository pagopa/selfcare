package it.pagopa.selfcare.onboarding.model;

import lombok.Data;
import wiremock.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@SuppressWarnings("java:S1068")
public class Aggregate {
    private String subunitCode;
    private String subunitType;
    private String description;
    private String parentDescription;
    private String recipientCode;
    private List<AggregateUser> users;
    private String digitalAddress;
    private String taxCode;
    private String vatNumber;
    private String address;
    private String city;
    private String county;
    private String zipCode;
    private String originId;
    private String origin;
    private String iban;

    @JsonIgnoreProperties
    private Integer rowNumber;
}
