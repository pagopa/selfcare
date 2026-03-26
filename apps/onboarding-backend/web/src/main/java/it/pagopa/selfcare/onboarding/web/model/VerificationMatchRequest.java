package it.pagopa.selfcare.onboarding.web.model;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class VerificationMatchRequest {



    @NotBlank
    private String taxCode;
    @Valid
    @NotNull
    private UserDto userDto;
}
