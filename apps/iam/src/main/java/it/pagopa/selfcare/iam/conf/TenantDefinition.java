package it.pagopa.selfcare.iam.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Tenant metadata shared across microservices via the {@code tenant.registry.json} configuration.
 * Only the fields relevant to iam are declared here; unknown properties (e.g. auth-specific ones
 * such as {@code authentication_provider}) are ignored so the same registry JSON can be reused
 * across services.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantDefinition(
    @JsonProperty("frontend_uri") String frontendUri,
    @JsonProperty("api_uri") String apiUri,
    @JsonProperty("allowed_origins") List<String> allowedOrigins) {}
