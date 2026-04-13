package it.pagopa.selfcare.onboarding.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class VerificationLegalAddressRequest {

    @NotBlank
    private String taxCode;
}
