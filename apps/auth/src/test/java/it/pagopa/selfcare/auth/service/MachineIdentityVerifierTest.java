package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.exception.MachineAuthenticationException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MachineIdentityVerifierTest {
  private static final String TENANT = "11111111-1111-4111-8111-111111111111";
  private static final String OID = "22222222-2222-4222-8222-222222222222";
  private KeyPair keys;
  private JwtConsumer consumer;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    keys = generator.generateKeyPair();
    consumer =
        MachineIdentityVerifier.consumer(TENANT, "auth-app-id", (jws, nesting) -> keys.getPublic());
  }

  @Test
  void acceptsSignedAppOnlyTokenForAuth() throws Exception {
    JwtClaims claims = consumer.processToClaims(sign(claims(), keys));
    assertEquals(OID, MachineIdentityVerifier.objectId(claims, TENANT));
  }

  @Test
  void rejectsWrongSignatureIssuerAudienceAndExpiredCredentials() throws Exception {
    KeyPair wrongKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    assertThrows(
        InvalidJwtException.class, () -> consumer.processToClaims(sign(claims(), wrongKey)));
    JwtClaims wrongAudience = claims();
    wrongAudience.setAudience("https://management.azure.com/");
    assertThrows(
        InvalidJwtException.class, () -> consumer.processToClaims(sign(wrongAudience, keys)));
    JwtClaims wrongIssuer = claims();
    wrongIssuer.setIssuer("https://login.microsoftonline.com/other/v2.0");
    assertThrows(
        InvalidJwtException.class, () -> consumer.processToClaims(sign(wrongIssuer, keys)));
    JwtClaims expired = claims();
    expired.setExpirationTimeMinutesInTheFuture(-2);
    assertThrows(InvalidJwtException.class, () -> consumer.processToClaims(sign(expired, keys)));
  }

  @Test
  void rejectsDelegatedTokensAndWrongDirectoryEvenWithValidSignature() throws Exception {
    JwtClaims delegated = claims();
    delegated.setStringClaim("idtyp", "user");
    assertThrows(
        MachineAuthenticationException.class,
        () ->
            MachineIdentityVerifier.objectId(
                consumer.processToClaims(sign(delegated, keys)), TENANT));
    JwtClaims wrongTenant = claims();
    wrongTenant.setStringClaim("tid", OID);
    assertThrows(
        MachineAuthenticationException.class,
        () ->
            MachineIdentityVerifier.objectId(
                consumer.processToClaims(sign(wrongTenant, keys)), TENANT));
  }

  private JwtClaims claims() {
    JwtClaims claims = new JwtClaims();
    claims.setIssuer("https://login.microsoftonline.com/" + TENANT + "/v2.0");
    claims.setAudience("auth-app-id");
    claims.setIssuedAtToNow();
    claims.setExpirationTimeMinutesInTheFuture(5);
    claims.setStringClaim("tid", TENANT);
    claims.setStringClaim("oid", OID);
    claims.setStringClaim("idtyp", "app");
    claims.setStringClaim("ver", "2.0");
    return claims;
  }

  private String sign(JwtClaims claims, KeyPair pair) throws Exception {
    JsonWebSignature jws = new JsonWebSignature();
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
    jws.setPayload(claims.toJson());
    jws.setKey(pair.getPrivate());
    return jws.getCompactSerialization();
  }
}
