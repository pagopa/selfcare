package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.config.JwtValidationConfig;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class HttpJwtValidatorTest {

  private static KeyPair arKeys;
  private static KeyPair pnpgKeys;
  private static KeyPair unknownKeys;

  private JwtValidationConfig config;
  private HttpJwtValidator validator;
  private ExecutionContext context;

  @BeforeAll
  static void generateKeys() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    arKeys = generator.generateKeyPair();
    pnpgKeys = generator.generateKeyPair();
    unknownKeys = generator.generateKeyPair();
  }

  @BeforeEach
  void setUp() {
    config = mock(JwtValidationConfig.class);
    when(config.allowedIssuers()).thenReturn(Set.of("SPID", "PAGOPA"));
    JwtValidationConfig.TenantKey arKey = tenantKey(pem(arKeys));
    JwtValidationConfig.TenantKey pnpgKey = tenantKey(pem(pnpgKeys));
    when(config.tenants()).thenReturn(Map.of("AR", arKey, "PNPG", pnpgKey));
    validator = new HttpJwtValidator(config, new ObjectMapper());
    context = mock(ExecutionContext.class);
    when(context.getLogger()).thenReturn(Logger.getGlobal());
  }

  @Test
  void acceptsValidTokenForHeaderTenant() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    Optional<HttpResponseMessage> response = validate(jwt, "AR");

    // then
    assertTrue(response.isEmpty());
  }

  @Test
  void acceptsCaseInsensitiveHeadersAndNormalizedTenant() {
    // given
    String jwt = token(arKeys, claims("PAGOPA", "user-id", "AR"), 60);
    Map<String, String> headers =
        Map.of("authorization", "bearer " + jwt, "x-tenant-id", " ar ");

    // when
    Optional<HttpResponseMessage> response = validator.validate(request(headers), context);

    // then
    assertTrue(response.isEmpty());
  }

  @Test
  void acceptsPemPublicKey() {
    // given
    String pem =
        "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder().encodeToString(arKeys.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----";
    JwtValidationConfig.TenantKey tenantKey = tenantKey(pem);
    when(config.tenants()).thenReturn(Map.of("AR", tenantKey));

    // when
    Optional<HttpResponseMessage> response =
        validate(token(arKeys, claims("SPID", "user-id", "AR"), 60), "AR");

    // then
    assertTrue(response.isEmpty());
  }

  @Test
  void rejectsMissingTenantHeaderWithBadRequest() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    Optional<HttpResponseMessage> response = validate(jwt, null);

    // then
    assertStatus(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsUnsupportedTenantHeaderWithBadRequest() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    Optional<HttpResponseMessage> response = validate(jwt, "UNKNOWN");

    // then
    assertStatus(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsMissingAuthorizationHeader() {
    // given
    HttpRequestMessage<Optional<String>> request = request(Map.of("X-Tenant-Id", "AR"));

    // when
    Optional<HttpResponseMessage> response = validator.validate(request, context);

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsNonBearerAuthorization() {
    // given
    HttpRequestMessage<Optional<String>> request =
        request(Map.of("X-Tenant-Id", "AR", "Authorization", "Basic credentials"));

    // when
    Optional<HttpResponseMessage> response = validator.validate(request, context);

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsMalformedOrUnsignedToken() {
    // given
    String unsigned =
        Jwts.builder()
            .setClaims(claims("SPID", "user-id", "AR"))
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .compact();

    // when / then
    assertStatus(validate("not-a-jwt", "AR"), HttpStatus.UNAUTHORIZED);
    assertStatus(validate(unsigned, "AR"), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsAlgorithmDifferentFromRs256() {
    // given
    String jwt =
        Jwts.builder()
            .setClaims(claims("SPID", "user-id", "AR"))
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(SignatureAlgorithm.HS256, arKeys.getPublic().getEncoded())
            .compact();

    // when
    Optional<HttpResponseMessage> response = validate(jwt, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsExpiredTokenAndTokenWithoutExpiration() {
    // given
    String expired = token(arKeys, claims("SPID", "user-id", "AR"), -60);
    String withoutExpiration =
        Jwts.builder()
            .setClaims(claims("SPID", "user-id", "AR"))
            .signWith(SignatureAlgorithm.RS256, arKeys.getPrivate())
            .compact();

    // when / then
    assertStatus(validate(expired, "AR"), HttpStatus.UNAUTHORIZED);
    assertStatus(validate(withoutExpiration, "AR"), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsWrongKeyIncludingOtherTenantKey() {
    // given
    String unknownKeyToken = token(unknownKeys, claims("SPID", "user-id", "AR"), 60);
    String otherTenantToken = token(pnpgKeys, claims("SPID", "user-id", "PNPG"), 60);

    // when / then
    assertStatus(validate(unknownKeyToken, "AR"), HttpStatus.UNAUTHORIZED);
    assertStatus(validate(otherTenantToken, "AR"), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsUnexpectedOrMissingIssuer() {
    // given
    String unexpected = token(arKeys, claims("OTHER", "user-id", "AR"), 60);
    String missing = token(arKeys, claims(null, "user-id", "AR"), 60);

    // when / then
    assertStatus(validate(unexpected, "AR"), HttpStatus.UNAUTHORIZED);
    assertStatus(validate(missing, "AR"), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsMissingUidOrTenantClaim() {
    // given
    String missingUid = token(arKeys, claims("SPID", null, "AR"), 60);
    String missingTenant = token(arKeys, claims("SPID", "user-id", null), 60);

    // when / then
    assertStatus(validate(missingUid, "AR"), HttpStatus.UNAUTHORIZED);
    assertStatus(validate(missingTenant, "AR"), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsTenantClaimDifferentFromHeader() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "PNPG"), 60);

    // when
    Optional<HttpResponseMessage> response = validate(jwt, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void failsClosedWhenTenantKeyIsMissingOrInvalid() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);
    JwtValidationConfig.TenantKey pnpgKey = tenantKey(pem(pnpgKeys));
    when(config.tenants()).thenReturn(Map.of("PNPG", pnpgKey));

    // when / then
    assertStatus(validate(jwt, "AR"), HttpStatus.UNAUTHORIZED);

    // given
    JwtValidationConfig.TenantKey invalidKey = tenantKey("not-a-key");
    when(config.tenants()).thenReturn(Map.of("AR", invalidKey));

    // when / then
    assertStatus(validate(jwt, "AR"), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void failsClosedWhenAllowedIssuersAreEmpty() {
    // given
    when(config.allowedIssuers()).thenReturn(Set.of());
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    Optional<HttpResponseMessage> response = validate(jwt, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  private Optional<HttpResponseMessage> validate(String jwt, String tenant) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + jwt);
    if (tenant != null) {
      headers.put("X-Tenant-Id", tenant);
    }
    return validator.validate(request(headers), context);
  }

  private static void assertStatus(
      Optional<HttpResponseMessage> response, HttpStatus expectedStatus) {
    assertTrue(response.isPresent());
    assertEquals(expectedStatus.value(), response.orElseThrow().getStatusCode());
  }

  @SuppressWarnings("unchecked")
  private static HttpRequestMessage<Optional<String>> request(Map<String, String> headers) {
    HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(headers);
    when(request.createResponseBuilder(any(HttpStatus.class)))
        .thenAnswer(
            (Answer<HttpResponseMessage.Builder>)
                invocation ->
                    new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                        .status((HttpStatus) invocation.getArguments()[0]));
    return request;
  }

  private static Map<String, Object> claims(String issuer, String uid, String tenant) {
    Map<String, Object> claims = new HashMap<>();
    if (issuer != null) {
      claims.put("iss", issuer);
    }
    if (uid != null) {
      claims.put("uid", uid);
    }
    if (tenant != null) {
      claims.put("tenant_id", tenant);
    }
    return claims;
  }

  private static String token(
      KeyPair keys, Map<String, Object> claims, long validForSeconds) {
    return Jwts.builder()
        .setClaims(claims)
        .setExpiration(new Date(System.currentTimeMillis() + validForSeconds * 1000))
        .signWith(SignatureAlgorithm.RS256, keys.getPrivate())
        .compact();
  }

  private static String pem(KeyPair keys) {
    return Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
  }

  private static JwtValidationConfig.TenantKey tenantKey(String publicKey) {
    JwtValidationConfig.TenantKey key = mock(JwtValidationConfig.TenantKey.class);
    when(key.publicKey()).thenReturn(Optional.ofNullable(publicKey));
    return key;
  }
}
