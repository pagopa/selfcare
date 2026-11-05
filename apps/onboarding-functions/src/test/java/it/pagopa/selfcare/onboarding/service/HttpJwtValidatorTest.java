package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import io.quarkus.test.junit.QuarkusTest;
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

@QuarkusTest
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
    when(config.defaultTenant()).thenReturn("PNPG");
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
    HttpAuthorization response = authenticate(jwt, "AR");

    // then
    assertFalse(response.isRejected());
  }

  @Test
  void exposesCallerIdentityFromVerifiedClaims() {
    // given
    Map<String, Object> claims = claims("SPID", "token-uid", "AR");
    claims.put("client_id", "apim");
    claims.put("subscription_id", "subscription-id");
    claims.put("product_id", " ");
    String jwt = token(arKeys, claims, 60);
    Map<String, String> headers =
        Map.of("Authorization", "Bearer " + jwt, "X-Tenant-Id", "AR", "x-selfcare-uid", "pippo");

    // when
    HttpAuthorization response = validator.authenticate(request(headers), context);

    // then
    assertFalse(response.isRejected());
    assertEquals("token-uid", response.caller().userId());
    assertEquals("SPID", response.caller().issuer());
    assertEquals("apim", response.caller().clientId());
    assertEquals("subscription-id", response.caller().subscriptionId());
    assertNull(response.caller().productId());
  }

  @Test
  void acceptsValidTokenForPnpgTenant() {
    // given
    String jwt = token(pnpgKeys, claims("PAGOPA", "fake-pnpg-user", "PNPG"), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "PNPG");

    // then
    assertFalse(response.isRejected());
  }

  @Test
  void acceptsCaseInsensitiveHeadersAndNormalizedTenant() {
    // given
    String jwt = token(arKeys, claims("PAGOPA", "user-id", "AR"), 60);
    Map<String, String> headers =
        Map.of("authorization", "bearer " + jwt, "x-tenant-id", " ar ");

    // when
    HttpAuthorization response = validator.authenticate(request(headers), context);

    // then
    assertFalse(response.isRejected());
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
    HttpAuthorization response =
        authenticate(token(arKeys, claims("SPID", "user-id", "AR"), 60), "AR");

    // then
    assertFalse(response.isRejected());
  }

  @Test
  void rejectsMissingTenantHeaderWithBadRequest() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    HttpAuthorization response = authenticate(jwt, null);

    // then
    assertStatus(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsUnsupportedTenantHeaderWithBadRequest() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "UNKNOWN");

    // then
    assertStatus(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsMissingAuthorizationHeader() {
    // given
    HttpRequestMessage<Optional<String>> request = request(Map.of("X-Tenant-Id", "AR"));

    // when
    HttpAuthorization response = validator.authenticate(request, context);

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
    assertEquals("Bearer", response.rejection().getHeader("WWW-Authenticate"));
    assertEquals("Unauthorized", response.rejection().getBody());
  }

  @Test
  void rejectsNonBearerAuthorization() {
    // given
    HttpRequestMessage<Optional<String>> request =
        request(Map.of("X-Tenant-Id", "AR", "Authorization", "Basic credentials"));

    // when
    HttpAuthorization response = validator.authenticate(request, context);

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

    // when
    HttpAuthorization malformedResponse = authenticate("not-a-jwt", "AR");
    HttpAuthorization unsignedResponse = authenticate(unsigned, "AR");

    // then
    assertStatus(malformedResponse, HttpStatus.UNAUTHORIZED);
    assertStatus(unsignedResponse, HttpStatus.UNAUTHORIZED);
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
    HttpAuthorization response = authenticate(jwt, "AR");

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

    // when
    HttpAuthorization expiredResponse = authenticate(expired, "AR");
    HttpAuthorization withoutExpirationResponse = authenticate(withoutExpiration, "AR");

    // then
    assertStatus(expiredResponse, HttpStatus.UNAUTHORIZED);
    assertStatus(withoutExpirationResponse, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsWrongKeyIncludingOtherTenantKey() {
    // given
    String unknownKeyToken = token(unknownKeys, claims("SPID", "user-id", "AR"), 60);
    String otherTenantToken = token(pnpgKeys, claims("SPID", "user-id", "PNPG"), 60);

    // when
    HttpAuthorization unknownKeyResponse = authenticate(unknownKeyToken, "AR");
    HttpAuthorization otherTenantResponse = authenticate(otherTenantToken, "AR");

    // then
    assertStatus(unknownKeyResponse, HttpStatus.UNAUTHORIZED);
    assertStatus(otherTenantResponse, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsUnexpectedOrMissingIssuer() {
    // given
    String unexpected = token(arKeys, claims("OTHER", "user-id", "AR"), 60);
    String missing = token(arKeys, claims(null, "user-id", "AR"), 60);

    // when
    HttpAuthorization unexpectedResponse = authenticate(unexpected, "AR");
    HttpAuthorization missingResponse = authenticate(missing, "AR");

    // then
    assertStatus(unexpectedResponse, HttpStatus.UNAUTHORIZED);
    assertStatus(missingResponse, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsMissingUid() {
    // given
    String missingUid = token(arKeys, claims("SPID", null, "AR"), 60);

    // when
    HttpAuthorization response = authenticate(missingUid, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void acceptsPnpgTokenWithoutTenantClaim() {
    // given
    String jwt = token(pnpgKeys, claims("SPID", "user-id", null), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "PNPG");

    // then
    assertFalse(response.isRejected());
  }

  @Test
  void rejectsTokenWithoutTenantClaimForNonDefaultTenant() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", null), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsTokenWithoutTenantClaimWhenClaimIsRequired() {
    // given
    when(config.tenantClaimRequired()).thenReturn(true);
    String jwt = token(pnpgKeys, claims("SPID", "user-id", null), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "PNPG");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsBlankOrNonStringTenantClaim() {
    // given
    Map<String, Object> numericTenant = claims("SPID", "user-id", null);
    numericTenant.put("tenant_id", 1);
    String blank = token(pnpgKeys, claims("SPID", "user-id", " "), 60);
    String numeric = token(pnpgKeys, numericTenant, 60);

    // when
    HttpAuthorization blankResponse = authenticate(blank, "PNPG");
    HttpAuthorization numericResponse = authenticate(numeric, "PNPG");

    // then
    assertStatus(blankResponse, HttpStatus.UNAUTHORIZED);
    assertStatus(numericResponse, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsTenantClaimDifferentFromHeader() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "PNPG"), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void failsClosedWhenTenantKeyIsMissingOrInvalid() {
    // given
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);
    JwtValidationConfig.TenantKey pnpgKey = tenantKey(pem(pnpgKeys));
    when(config.tenants()).thenReturn(Map.of("PNPG", pnpgKey));

    // when
    HttpAuthorization missingKeyResponse = authenticate(jwt, "AR");

    // then
    assertStatus(missingKeyResponse, HttpStatus.UNAUTHORIZED);

    // given
    JwtValidationConfig.TenantKey invalidKey = tenantKey("not-a-key");
    when(config.tenants()).thenReturn(Map.of("AR", invalidKey));

    // when
    HttpAuthorization invalidKeyResponse = authenticate(jwt, "AR");

    // then
    assertStatus(invalidKeyResponse, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void failsClosedWhenAllowedIssuersAreEmpty() {
    // given
    when(config.allowedIssuers()).thenReturn(Set.of());
    String jwt = token(arKeys, claims("SPID", "user-id", "AR"), 60);

    // when
    HttpAuthorization response = authenticate(jwt, "AR");

    // then
    assertStatus(response, HttpStatus.UNAUTHORIZED);
  }

  private HttpAuthorization authenticate(String jwt, String tenant) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + jwt);
    if (tenant != null) {
      headers.put("X-Tenant-Id", tenant);
    }
    return validator.authenticate(request(headers), context);
  }

  private static void assertStatus(
      HttpAuthorization response, HttpStatus expectedStatus) {
    assertTrue(response.isRejected());
    assertEquals(expectedStatus.value(), response.rejection().getStatusCode());
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
