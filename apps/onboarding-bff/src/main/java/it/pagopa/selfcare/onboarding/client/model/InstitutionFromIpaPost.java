package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class InstitutionFromIpaPost {

    @NotNull
    private String taxCode;
    private String subunitCode;
    private String subunitType;
}
