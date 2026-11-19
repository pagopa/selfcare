package it.pagopa.selfcare.auth.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MachineTokenRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String tenantId) {}
