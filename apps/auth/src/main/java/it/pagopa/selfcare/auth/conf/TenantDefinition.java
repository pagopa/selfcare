package it.pagopa.selfcare.auth.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantDefinition(
    @JsonProperty("frontend_uri") String frontendUri,
    @JsonProperty("api_uri") String apiUri,
    @JsonProperty("allowed_origins") List<String> allowedOrigins,
    @JsonProperty("authentication_provider") String authenticationProvider,
    @JsonProperty("auth_enabled") boolean authEnabled,
    @JsonProperty("jwt") JwtDefinition jwt) {

  public TenantDefinition(
      String frontendUri,
      String apiUri,
      List<String> allowedOrigins,
      String authenticationProvider,
      boolean authEnabled) {
    this(frontendUri, apiUri, allowedOrigins, authenticationProvider, authEnabled, null);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record JwtDefinition(
      @JsonProperty("publicKeyEnvVar") String publicKeyEnvVar,
      @JsonProperty("session") SessionDefinition session) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SessionDefinition(
      @JsonProperty("privateKeyEnvVar") String privateKeyEnvVar,
      @JsonProperty("keyIdEnvVar") String keyIdEnvVar) {}
}
