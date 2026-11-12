package it.pagopa.selfcare.auth.service;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import org.eclipse.microprofile.config.Config;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MachineTokenTenantTest.PnpgProfile.class)
class MachineTokenTenantTest {
  private static final String OID = "22222222-2222-4222-8222-222222222222";
  private static final String AUDIENCE = "api-pnpg.example.test";

  @InjectMock MachineIdentityVerifier verifier;
  @Inject TenantRegistry tenants;
  @Inject TenantSessionKeyProvider keys;
  @Inject SessionService sessions;
  @Inject Config config;

  @Test
  void issuesPnpgTokenUsingRegistryKeyWithoutLoginOrLoginResources() throws Exception {
    when(verifier.verify("Bearer test-identity")).thenReturn(OID);

    String token =
        given()
            .header("Authorization", "Bearer test-identity")
            .contentType("application/json")
            .body(
                """
                {"tenantId":"PNPG","audience":"user-ms","scope":"user.read"}
                """)
            .post("/token/service")
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");

    var publicKey =
        KeyFactory.getInstance("RSA")
            .generatePublic(
                new X509EncodedKeySpec(
                    Base64.getDecoder()
                        .decode(config.getValue("TEST_PNPG_PUBLIC_KEY", String.class))));
    var claims =
        new JwtConsumerBuilder()
            .setVerificationKey(publicKey)
            .setExpectedIssuer("PAGOPA")
            .setExpectedAudience(AUDIENCE)
            .setRequireExpirationTime()
            .build()
            .processToClaims(token);
    assertEquals("PNPG", claims.getStringClaimValue("tenant_id"));
    assertEquals("pnpg-caller", claims.getStringClaimValue("uid"));
    assertEquals(300, claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue());
    JsonWebSignature jws = new JsonWebSignature();
    jws.setCompactSerialization(token);
    assertEquals("pnpg-test-kid", jws.getKeyIdHeaderValue());
    assertNotEquals(keys.getSigningKey("AR").privateKey(), keys.getSigningKey("PNPG").privateKey());
    assertThrows(
        InvalidJwtException.class,
        () ->
            new JwtConsumerBuilder()
                .setVerificationKey(
                    KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic())
                .setExpectedIssuer("PAGOPA")
                .setExpectedAudience(AUDIENCE)
                .build()
                .processToClaims(token));

    assertFalse(tenants.resolveTenant("PNPG").definition().authEnabled());
    assertThrows(ForbiddenException.class, () -> tenants.resolveEnabledTenant("PNPG"));
    assertThrows(ForbiddenException.class, () -> tenants.oneIdentityCredentials("PNPG"));
    UserClaims user = UserClaims.builder().uid("user").tenantId("PNPG").build();
    assertThrows(ForbiddenException.class, () -> sessions.generateSessionToken(user));
    assertThrows(ForbiddenException.class, () -> sessions.generateSessionTokenInternal(user));
  }

  @Test
  void registrySigningConfigurationDoesNotGrantMachineAccess() {
    when(verifier.verify("Bearer test-identity")).thenReturn(OID);

    given()
        .header("Authorization", "Bearer test-identity")
        .contentType("application/json")
        .body(
            """
            {"tenantId":"AR","audience":"user-ms","scope":"user.read"}
            """)
        .post("/token/service")
        .then()
        .statusCode(403);
  }

  public static class PnpgProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      try (InputStream input =
          MachineTokenTenantTest.class.getResourceAsStream("/application-test.properties")) {
        Properties properties = new Properties();
        properties.load(input);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode registry =
            (ObjectNode) mapper.readTree(properties.getProperty("tenant.registry.json"));
        // PNPG only signs tokens: no OIDC, mail, User Registry or Mongo configuration.
        registry.set(
            "PNPG",
            mapper.readTree(
                """
                {"auth_enabled":false,"jwt":{"session":{
                  "privateKeyEnvVar":"TEST_PNPG_PRIVATE_KEY",
                  "keyIdEnvVar":"TEST_PNPG_KEY_ID",
                  "audience":"api-pnpg.example.test"
                }}}
                """));
        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String pem =
            "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        return Map.ofEntries(
            Map.entry("tenant.registry.json", mapper.writeValueAsString(registry)),
            Map.entry("tenant.supported-tenants", "AR"),
            Map.entry("jwt.session.tenant-audience-enabled", "true"),
            Map.entry("TEST_PNPG_PRIVATE_KEY", pem),
            Map.entry("TEST_PNPG_KEY_ID", "pnpg-test-kid"),
            Map.entry(
                "TEST_PNPG_PUBLIC_KEY",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())),
            Map.entry("auth.m2m.enabled", "true"),
            Map.entry("auth.m2m.entra-tenant-id", "11111111-1111-4111-8111-111111111111"),
            Map.entry("auth.m2m.entra-audience", "test-token-api"),
            Map.entry(
                "auth.m2m.grants",
                """
                [{"objectId":"22222222-2222-4222-8222-222222222222",
                  "serviceId":"pnpg-caller","tenantId":"PNPG",
                  "audience":"user-ms","scope":"user.read"}]
                """));
      } catch (IOException | GeneralSecurityException exception) {
        throw new IllegalStateException("Cannot configure PNPG machine-token test", exception);
      }
    }
  }
}
