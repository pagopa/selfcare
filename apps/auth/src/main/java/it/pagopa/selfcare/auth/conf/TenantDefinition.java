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
    @JsonProperty("jwt") JwtDefinition jwt,
    @JsonProperty("m2m") MachineDefinition m2m) {

  public TenantDefinition(
      String frontendUri,
      String apiUri,
      List<String> allowedOrigins,
      String authenticationProvider,
      boolean authEnabled,
      JwtDefinition jwt) {
    this(frontendUri, apiUri, allowedOrigins, authenticationProvider, authEnabled, jwt, null);
  }

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
      @JsonProperty("keyIdEnvVar") String keyIdEnvVar,
      @JsonProperty("audience") String audience) {}

  public record MachineDefinition(
      boolean enabled,
      String uidEnvVar,
      String iamBaseUrl,
      List<String> groups,
      Integer lifetimeSeconds,
      List<MachineGrant> grants) {
    public MachineDefinition {
      lifetimeSeconds = lifetimeSeconds == null ? 300 : lifetimeSeconds;
      groups = groups == null ? List.of() : List.copyOf(groups);
      grants = grants == null ? List.of() : List.copyOf(grants);
    }
  }

  public record MachineGrant(String audience, String scope, String productId) {}
}
