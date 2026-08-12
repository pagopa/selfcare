package it.pagopa.selfcare.onboarding.client.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the tenant out of the machine JWT this consumer sends on service-to-service calls
 * (Step_0 SELC-5).
 *
 * <p>Downstream services reject with 400 any request carrying a JWT but no {@code X-Tenant-Id}
 * header, and equally reject a header that disagrees with the token's {@code tenant_id} claim. The
 * header must therefore be derived from the very token being sent rather than from a separate
 * configuration value, which could silently drift out of sync with the provisioned token and turn
 * every call into a tenant-mismatch rejection.
 *
 * <p>The token is <b>not</b> verified here: it is not being trusted, only read back. Verification
 * is the receiving service's job.
 */
final class MachineTokenTenantResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(MachineTokenTenantResolver.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** JWT claim carrying the tenant identifier; mirrors {@code TenantConstants#TENANT_CLAIM}. */
  private static final String TENANT_CLAIM = "tenant_id";

  private MachineTokenTenantResolver() {}

  static Optional<String> tenantOf(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return Optional.empty();
    }
    String[] segments = rawToken.split("\\.");
    if (segments.length < 2) {
      LOGGER.warn("JWT_BEARER_TOKEN is not a well-formed JWT; no tenant header will be sent");
      return Optional.empty();
    }
    try {
      byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
      JsonNode claim = MAPPER.readTree(new String(payload, StandardCharsets.UTF_8)).get(TENANT_CLAIM);
      if (claim == null || claim.asText().isBlank()) {
        LOGGER.warn(
            "JWT_BEARER_TOKEN carries no {} claim: downstream tenant validation will reject these"
                + " calls until the token is re-issued with the claim",
            TENANT_CLAIM);
        return Optional.empty();
      }
      return Optional.of(claim.asText());
    } catch (Exception e) {
      LOGGER.warn("Could not read the tenant claim from JWT_BEARER_TOKEN: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
