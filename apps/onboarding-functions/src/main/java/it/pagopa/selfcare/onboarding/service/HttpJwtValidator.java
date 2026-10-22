package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import it.pagopa.selfcare.onboarding.config.JwtValidationConfig;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Authenticates the bearer JWT of an HTTP-triggered function request. */
@ApplicationScoped
public class HttpJwtValidator {

  static final String AUTHORIZATION_HEADER = "Authorization";
  static final String UID_CLAIM = "uid";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String EXPECTED_ALGORITHM = "RS256";

  private final JwtValidationConfig config;
  private final ObjectMapper objectMapper;

  public HttpJwtValidator(JwtValidationConfig config, ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
  }

  /**
   * Validates both the tenant header and the JWT. The returned response must be sent immediately;
   * an empty result means that the request is authenticated.
   */
  public Optional<HttpResponseMessage> validate(
      HttpRequestMessage<?> request, ExecutionContext context) {
    final String headerTenant;
    try {
      headerTenant = TenantContext.resolve(TenantContext.tenantHeader(request));
    } catch (IllegalArgumentException exception) {
      context.getLogger().warning("Rejected request with missing or unsupported tenant");
      return Optional.of(
          request
              .createResponseBuilder(HttpStatus.BAD_REQUEST)
              .body("Invalid tenant context")
              .build());
    }

    try {
      authenticate(request, headerTenant);
      return Optional.empty();
    } catch (JwtException | IllegalArgumentException exception) {
      context.getLogger().warning(() -> "Rejected request with invalid JWT: " + exception.getMessage());
      return Optional.of(
          request
              .createResponseBuilder(HttpStatus.UNAUTHORIZED)
              .header("WWW-Authenticate", "Bearer")
              .body("Unauthorized")
              .build());
    }
  }

  private void authenticate(HttpRequestMessage<?> request, String headerTenant) {
    String token = extractBearerToken(request);
    requireAlgorithm(token);

    Claims claims =
        Jwts.parser()
            .setSigningKey(verificationKey(headerTenant))
            .parseClaimsJws(token)
            .getBody();

    if (claims.getExpiration() == null) {
      throw new JwtException("Missing exp claim");
    }
    if (isBlank(claims.getIssuer()) || !allowedIssuers().contains(claims.getIssuer())) {
      throw new JwtException("Unexpected issuer");
    }
    if (isBlank(claims.get(UID_CLAIM, String.class))) {
      throw new JwtException("Missing uid claim");
    }

    String claimTenant = normalize(claims.get(TenantContext.TENANT_CLAIM, String.class));
    if (claimTenant == null) {
      throw new JwtException("Missing tenant_id claim");
    }
    if (!claimTenant.equals(headerTenant)) {
      throw new JwtException("Tenant claim does not match tenant header");
    }
  }

  private String extractBearerToken(HttpRequestMessage<?> request) {
    Map<String, String> headers = request.getHeaders();
    String authorization =
        headers == null
            ? null
            : headers.entrySet().stream()
                .filter(entry -> AUTHORIZATION_HEADER.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    if (authorization == null
        || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      throw new JwtException("Missing bearer token");
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new JwtException("Missing bearer token");
    }
    return token;
  }

  private void requireAlgorithm(String token) {
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3) {
      throw new JwtException("Malformed token");
    }
    try {
      JsonNode header =
          objectMapper.readTree(
              new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
      if (!EXPECTED_ALGORITHM.equals(header.path("alg").asText())) {
        throw new JwtException("Unexpected signing algorithm");
      }
    } catch (JwtException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new JwtException("Malformed token header");
    }
  }

  private PublicKey verificationKey(String tenant) {
    JwtValidationConfig.TenantKey tenantKey =
        config.tenants().entrySet().stream()
            .filter(entry -> tenant.equals(normalize(entry.getKey())))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> new JwtException("No verification key configured for tenant"));
    String publicKey =
        tenantKey
            .publicKey()
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new JwtException("No verification key configured for tenant"));
    return toPublicKey(publicKey);
  }

  private PublicKey toPublicKey(String pem) {
    try {
      String base64 = pem.replaceAll("-----(BEGIN|END)[A-Z ]*-----", "").replaceAll("\\s", "");
      return KeyFactory.getInstance("RSA")
          .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    } catch (Exception exception) {
      throw new IllegalArgumentException("Invalid JWT public key configuration");
    }
  }

  private Set<String> allowedIssuers() {
    Set<String> issuers =
        config.allowedIssuers().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    if (issuers.isEmpty()) {
      throw new IllegalArgumentException("No JWT issuer configured");
    }
    return issuers;
  }

  private static String normalize(String tenant) {
    return isBlank(tenant) ? null : tenant.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
