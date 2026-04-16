package it.pagopa.selfcare.onboarding.model;

import lombok.Data;

@Data
public class AggregateUser {
    private String name;
    private String surname;
    private String taxCode;
    private String email;
    private String role;
}