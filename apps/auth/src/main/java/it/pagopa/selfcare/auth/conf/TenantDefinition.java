package it.pagopa.selfcare.auth.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TenantDefinition(
    @JsonProperty("frontend_uri") String frontendUri,
    @JsonProperty("api_uri") String apiUri,
    @JsonProperty("allowed_origins") List<String> allowedOrigins,
    @JsonProperty("authentication_provider") String authenticationProvider,
    @JsonProperty("auth_enabled") boolean authEnabled) {}
