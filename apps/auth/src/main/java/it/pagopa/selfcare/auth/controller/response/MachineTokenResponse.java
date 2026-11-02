package it.pagopa.selfcare.auth.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MachineTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    String scope) {}
