package it.pagopa.selfcare.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.ParseException;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;

/**
 * Verifies tokens issued by different tenants that sign with distinct keys (e.g. {@code AR} and
 * {@code PNPG} in a consolidated deployment). The verification key material is resolved, in
 * order:
 *
 * <ul>
 *   <li>from the {@link TenantRegistry}: for every tenant that configures a {@code
 *       jwt.publicKeyEnvVar}, the referenced environment variable is loaded and its key(s) merged
 *       into a single {@code kid -> key} map (a plain PEM key is stored under a per-tenant
 *       synthetic kid, {@code tenant:<TENANT_ID>}, so distinct tenants' PEM keys never collide);
 *   <li>otherwise, from the legacy {@code mp.jwt.verify.publickey} property, exactly as before:
 *       either a single PEM key (matched regardless of the token's {@code kid}) or a JWK/JWKS
 *       document (matched by {@code kid}).
 * </ul>
 *
 * <p>Today every deployment is single-tenant, so the tenant-registry path yields exactly one key
 * and verification behaves identically to the legacy path (the token's {@code kid} is ignored,
 * see {@link #selectCandidateKeys}). This is deliberate groundwork for a future consolidated
 * deployment serving multiple tenants at once: a plain PEM tenant key has no real {@code kid}, so
 * when multiple such keys coexist and the token carries none either, every key is tried in turn
 * until one verifies (see {@link #selectCandidateKeys} for the exact rules, including the case
 * where a token does carry a {@code kid}).
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class JWTCallerPrincipalFactory extends DefaultJWTCallerPrincipalFactory {

  private static final Logger LOG = Logger.getLogger(JWTCallerPrincipalFactory.class);

  private final Set<String> validIssuers = Set.of("SPID", "PAGOPA");

  /** Keyed by {@code kid}; empty/synthetic key when the config holds a single legacy PEM key. */
  final Map<String, PublicKey> publicKeysByKid;

  @Inject
  public JWTCallerPrincipalFactory(
      @ConfigProperty(name = "mp.jwt.verify.publickey", defaultValue = "NONE") String pubKey,
      TenantRegistry tenantRegistry)
      throws Exception {
    publicKeysByKid = loadPublicKeys(isNoneOrBlank(pubKey) ? "" : pubKey, tenantRegistry);
  }

  private static boolean isNoneOrBlank(String value) {
    return value == null || value.isBlank() || "NONE".equals(value);
  }

  /** Convenience constructor for unit tests exercising the legacy, non-tenant-aware path. */
  JWTCallerPrincipalFactory(String pubKey) throws Exception {
    publicKeysByKid = loadPublicKeys(pubKey, null);
  }

  @Override
  public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
    try {
      String issuer = extractIssuerFromJwt(token);
      if (!validIssuers.contains(issuer)) {
        throw new ParseException("Invalid issuer: " + issuer);
      }

      List<PublicKey> candidateKeys = selectCandidateKeys(extractKidFromJwt(token));

      ParseException lastFailure = null;
      for (PublicKey candidateKey : candidateKeys) {
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(authContextInfo);
        contextInfo.setPublicVerificationKey(candidateKey);
        contextInfo.setIssuedBy(issuer);
        contextInfo.setRequiredClaims(Set.of("uid"));
        contextInfo.setDefaultSubjectClaim("uid");
        try {
          return super.parse(token, contextInfo);
        } catch (Exception e) {
          // Signature didn't match this candidate key; try the next one (relevant only when the
          // token carries no kid and multiple tenant keys are configured, see
          // selectCandidateKeys). The final failure is surfaced only once every candidate fails.
          lastFailure = e instanceof ParseException pe ? pe : new ParseException("Token validation failed");
        }
      }
      throw lastFailure != null ? lastFailure : new ParseException("Token validation failed");

    } catch (Exception e) {
      throw new ParseException("Token validation failed");
    }
  }

  /**
   * Resolves the verification key(s) to attempt for a token carrying the given {@code kid}
   * (header value, or {@code null} if absent).
   *
   * <ul>
   *   <li>Single-key deployment (legacy PEM, JWKS with one entry, or exactly one tenant
   *       configured): that key is used regardless of the token's {@code kid}, preserving
   *       pre-existing behavior.
   *   <li>Multiple keys and a non-null {@code kid}: only the exact match is tried, and an unknown
   *       {@code kid} is rejected outright. A token that explicitly names a key id we don't
   *       recognize is either misconfigured or suspicious, so it must not silently fall through
   *       to trying unrelated keys.
   *   <li>Multiple keys and no {@code kid} at all (the common case today: plain PEM tenant keys,
   *       which have no real {@code kid} and are stored under a synthetic {@code tenant:<ID>}
   *       placeholder that never appears in a real token): every configured key is tried in turn,
   *       and the first one whose signature actually validates is used. This keeps a consolidated
   *       multi-tenant deployment working without requiring every tenant to migrate to
   *       JWKS-with-real-kid up front, at the cost of verifying the signature once per candidate
   *       key.
   * </ul>
   */
  List<PublicKey> selectCandidateKeys(String kid) throws ParseException {
    if (publicKeysByKid.size() == 1) {
      return List.copyOf(publicKeysByKid.values());
    }
    if (kid != null) {
      PublicKey key = publicKeysByKid.get(kid);
      if (key == null) {
        throw new ParseException("No matching verification key for kid: " + kid);
      }
      return List.of(key);
    }
    if (publicKeysByKid.isEmpty()) {
      throw new ParseException("No matching verification key for kid: null");
    }
    return List.copyOf(publicKeysByKid.values());
  }

  /**
   * Resolves the verification key map from the {@link TenantRegistry} when at least one tenant
   * configures a {@code jwt.publicKeyEnvVar}, otherwise falls back to the legacy {@code
   * mp.jwt.verify.publickey} value.
   */
  static Map<String, PublicKey> loadPublicKeys(String legacyPubKey, TenantRegistry tenantRegistry)
      throws Exception {
    Map<String, PublicKey> tenantKeys = loadTenantPublicKeys(tenantRegistry);
    if (!tenantKeys.isEmpty()) {
      LOG.infof(
          "JWT verification key(s) loaded from tenant registry: %d key(s), kid(s)=%s",
          tenantKeys.size(), tenantKeys.keySet());
      return tenantKeys;
    }
    if (legacyPubKey == null || legacyPubKey.isBlank()) {
      throw new IllegalStateException(
          "No JWT verification key configured: set mp.jwt.verify.publickey, or configure a "
              + "tenant.registry.json entry with a jwt.publicKeyEnvVar for at least one tenant.");
    }
    Map<String, PublicKey> legacyKeys = loadPublicKeys(legacyPubKey);
    LOG.infof(
        "No tenant in tenant.registry.json configures a jwt.publicKeyEnvVar (or no "
            + "TenantRegistry is available); falling back to legacy mp.jwt.verify.publickey: "
            + "%d key(s), kid(s)=%s",
        legacyKeys.size(), legacyKeys.keySet());
    return legacyKeys;
  }

  private static Map<String, PublicKey> loadTenantPublicKeys(TenantRegistry tenantRegistry)
      throws Exception {
    if (tenantRegistry == null) {
      return Map.of();
    }
    Map<String, PublicKey> keys = new HashMap<>();
    for (String tenantId : tenantRegistry.supportedTenantIds()) {
      Optional<String> keyMaterial = tenantRegistry.jwtPublicKey(tenantId);
      if (keyMaterial.isEmpty()) {
        LOG.infof(
            "Tenant '%s' has no jwt.publicKeyEnvVar configured (or its env var is unset); "
                + "skipping it for JWT key resolution",
            tenantId);
        continue;
      }
      Map<String, PublicKey> parsed = loadPublicKeys(keyMaterial.get());
      for (Map.Entry<String, PublicKey> entry : parsed.entrySet()) {
        // A plain PEM key (parsed under a null kid) gets a per-tenant synthetic kid so that
        // multiple tenants' PEM keys can coexist in the merged map without colliding.
        String kid = entry.getKey() == null ? "tenant:" + tenantId : entry.getKey();
        if (keys.containsKey(kid)) {
          throw new IllegalStateException(
              "Duplicate JWT key id '"
                  + kid
                  + "' across tenants; each tenant's key(s) must use a distinct kid when more "
                  + "than one tenant is configured.");
        }
        LOG.infof("Tenant '%s' contributes JWT verification key with kid '%s'", tenantId, kid);
        keys.put(kid, entry.getValue());
      }
    }
    return keys;
  }

  /**
   * Parses a single tenant's (or the legacy) key material as a JWKS, falling back to a single JWK
   * and finally to a raw PEM key.
   */
  static Map<String, PublicKey> loadPublicKeys(String pubKey) throws Exception {
    String trimmed = pubKey.trim();
    if (trimmed.startsWith("{")) {
      Map<String, PublicKey> keys = new HashMap<>();
      if (trimmed.contains("\"keys\"")) {
        JsonWebKeySet keySet = new JsonWebKeySet(trimmed);
        for (JsonWebKey jwk : keySet.getJsonWebKeys()) {
          keys.put(jwk.getKeyId(), toPublicKey(jwk));
        }
      } else {
        JsonWebKey jwk = JsonWebKey.Factory.newJwk(trimmed);
        keys.put(jwk.getKeyId(), toPublicKey(jwk));
      }
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("mp.jwt.verify.publickey JWK(S) does not contain any key");
      }
      return Map.copyOf(keys);
    }
    // Legacy single PEM key: stored under a null key id, selected whenever only one key is configured.
    Map<String, PublicKey> keys = new HashMap<>();
    keys.put(null, createPublicKeyFromString(trimmed, "RSA"));
    return keys;
  }

  private static PublicKey toPublicKey(JsonWebKey jwk) {
    if (!(jwk instanceof PublicJsonWebKey publicJsonWebKey)) {
      throw new IllegalArgumentException("JWK " + jwk.getKeyId() + " does not contain a public key");
    }
    return publicJsonWebKey.getPublicKey();
  }

  public static PublicKey createPublicKeyFromString(String publicKeyString, String algorithm) throws Exception {
    String publicKeyPEM = cleanKeyString(publicKeyString);
    byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
    return keyFactory.generatePublic(keySpec);
  }

  public static String cleanKeyString(String publicKeyString) {
    return publicKeyString
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replaceAll("\\s", "");
  }

  public String extractIssuerFromJwt(String token) throws Exception {
    return jwtSegmentAsJson(token, 1).get("iss").asText();
  }

  public String extractKidFromJwt(String token) throws Exception {
    JsonNode header = jwtSegmentAsJson(token, 0);
    JsonNode kid = header.get("kid");
    return kid == null ? null : kid.asText();
  }

  private JsonNode jwtSegmentAsJson(String token, int index) throws Exception {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT format");
    }
    String segment = new String(Base64.getUrlDecoder().decode(parts[index]));
    return new ObjectMapper().readTree(segment);
  }
}
