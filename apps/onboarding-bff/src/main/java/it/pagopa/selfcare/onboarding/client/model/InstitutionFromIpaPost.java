package it.pagopa.selfcare.onboarding.client.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstitutionFromIpaPost {

    @NotNull
    private String taxCode;
    private String subunitCode;
    private String subunitType;
}
