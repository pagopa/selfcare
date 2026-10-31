package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.controller.response.MachineTokenResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MachineTokenServiceTest {
  private static final String OID = "22222222-2222-4222-8222-222222222222";
  private static final String GRANTS =
      """
      [{"objectId":"22222222-2222-4222-8222-222222222222","serviceId":"onboarding-cdc",
        "tenantId":"AR","audience":"onboarding-functions","scope":"onboarding-functions.invoke"}]
      """;
  private MachineTokenService service;
  private KeyPair keys;

  @BeforeEach
  void setUp() throws Exception {
    service = new MachineTokenService();
    service.config = mock(MachineTokenConfig.class);
    when(service.config.enabled()).thenReturn(true);
    when(service.config.lifetime()).thenReturn(Duration.ofMinutes(5));
    when(service.config.grants()).thenReturn(GRANTS);
    service.mapper = new ObjectMapper();
    service.verifier = mock(MachineIdentityVerifier.class);
    when(service.verifier.verify("Bearer entra-token")).thenReturn(OID);
    service.tenants = mock(TenantRegistry.class);
    service.keys = mock(TenantSessionKeyProvider.class);
    keys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    when(service.keys.getSigningKey("AR"))
        .thenReturn(new TenantSessionKeyProvider.SigningKey(keys.getPrivate(), "test-kid"));
    service.initialize();
  }

  @Test
  void issuesShortLivedServiceTokenWithOnlyGrantedClaims() throws Exception {
    MachineTokenResponse response = service.issue("Bearer entra-token", request("AR"));
    JwtClaims claims =
        new JwtConsumerBuilder()
            .setVerificationKey(keys.getPublic())
            .setExpectedIssuer("SELFCARE-M2M")
            .setExpectedAudience("onboarding-functions")
            .setRequireExpirationTime()
            .build()
            .processToClaims(response.accessToken());
    assertEquals("onboarding-cdc", claims.getSubject());
    assertEquals("onboarding-cdc", claims.getStringClaimValue("uid"));
    assertEquals("AR", claims.getStringClaimValue("tenant_id"));
    assertEquals("service", claims.getStringClaimValue("token_use"));
    assertEquals("onboarding-functions.invoke", claims.getStringClaimValue("scope"));
    assertEquals(300, claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue());
    assertFalse(claims.hasClaim("fiscal_number"));
    assertFalse(claims.hasClaim("spid_level"));
    assertEquals(300, response.expiresIn());
  }

  @Test
  void deniesOtherTenantAudienceScopeAndUnknownPrincipalBeforeSigning() {
    assertThrows(
        ForbiddenException.class, () -> service.issue("Bearer entra-token", request("PNPG")));
    assertThrows(
        ForbiddenException.class,
        () ->
            service.issue(
                "Bearer entra-token",
                new MachineTokenRequest("AR", "user-ms", "onboarding-functions.invoke")));
    assertThrows(
        ForbiddenException.class,
        () ->
            service.issue(
                "Bearer entra-token",
                new MachineTokenRequest("AR", "onboarding-functions", "admin")));
    when(service.verifier.verify("Bearer entra-token")).thenReturn("unknown");
    assertThrows(
        ForbiddenException.class, () -> service.issue("Bearer entra-token", request("AR")));
    verifyNoInteractions(service.keys);
  }

  @Test
  void disabledIssuerDoesNotAuthenticateOrSign() {
    when(service.config.enabled()).thenReturn(false);
    assertThrows(
        ResourceNotFoundException.class, () -> service.issue("Bearer entra-token", request("AR")));
    verifyNoInteractions(service.verifier, service.keys);
  }

  @Test
  void rejectsAmbiguousPolicyAndExcessiveLifetime() {
    when(service.config.grants())
        .thenReturn(
            "["
                + GRANTS.substring(1, GRANTS.lastIndexOf(']'))
                + ","
                + GRANTS.substring(1, GRANTS.lastIndexOf(']'))
                + "]");
    assertThrows(IllegalStateException.class, service::initialize);
    when(service.config.lifetime()).thenReturn(Duration.ofHours(1));
    assertThrows(IllegalStateException.class, service::initialize);
  }

  private MachineTokenRequest request(String tenant) {
    return new MachineTokenRequest(tenant, "onboarding-functions", "onboarding-functions.invoke");
  }
}
