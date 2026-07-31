package it.pagopa.selfcare.onboarding.client.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

final class MachineTokenTenantResolver {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MachineTokenTenantResolver() {}

  static Optional<String> tenantOf(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return Optional.empty();
    }
    String[] segments = rawToken.split("\\.");
    if (segments.length < 2) {
      return Optional.empty();
    }
    try {
      byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
      JsonNode claim =
          MAPPER
              .readTree(new String(payload, StandardCharsets.UTF_8))
              .get(FunctionTenantContext.TENANT_CLAIM);
      if (claim == null || claim.asText().isBlank()) {
        return Optional.empty();
      }
      return Optional.of(claim.asText());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
