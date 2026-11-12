package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.conf.*;
import it.pagopa.selfcare.auth.conf.TenantDefinition.*;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.security.KeyPairGenerator;
import java.util.List;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MachineTokenServiceTest {
  static final String UID = "22222222-2222-4222-8222-222222222222";
  private MachineTokenService service;
  private TenantRegistry.Tenant tenant;

  @BeforeEach
  void setUp() throws Exception {
    service = new MachineTokenService();
    service.config = mock(MachineTokenConfig.class);
    when(service.config.enabled()).thenReturn(true);
    service.tenants = mock(TenantRegistry.class);
    service.keys = mock(TenantSessionKeyProvider.class);
    service.sessions = new SessionServiceImpl();
    service.sessions.audience = "api.test";
    service.iam = mock(MachineIamAuthorizer.class);
    tenant =
        tenant(
            new MachineDefinition(
                true,
                UID,
                "https://iam.test",
                List.of("SUPPORT"),
                300,
                List.of(new MachineGrant("user-ms", "user.read", "prod-test"))));
    when(service.tenants.machineTenants()).thenReturn(List.of(tenant));
    when(service.tenants.resolveTenant("PNPG")).thenReturn(tenant);
    var pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    when(service.keys.getSigningKey("PNPG"))
        .thenReturn(new TenantSessionKeyProvider.SigningKey(pair.getPrivate(), "pnpg-kid"));
  }

  @Test
  void issuesConfiguredIamIdentityOnlyAfterPermissionCheck() throws Exception {
    service.initialize();
    var pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    when(service.keys.getSigningKey("PNPG"))
        .thenReturn(new TenantSessionKeyProvider.SigningKey(pair.getPrivate(), "pnpg-kid"));
    var response = service.issue(request("user.read"));
    var claims =
        new JwtConsumerBuilder()
            .setVerificationKey(pair.getPublic())
            .setExpectedIssuer("PAGOPA")
            .setExpectedAudience("api.test")
            .setRequireExpirationTime()
            .build()
            .processToClaims(response.accessToken());
    assertEquals(UID, claims.getSubject());
    assertEquals(UID, claims.getStringClaimValue("uid"));
    assertEquals("PNPG", claims.getStringClaimValue("tenant_id"));
    assertEquals(List.of("SUPPORT"), claims.getStringListClaimValue("groups"));
    assertEquals(300, claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue());
    assertEquals(300, response.expiresIn());
    var token = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(service.iam)
        .requirePermission(eq("PNPG"), eq(tenant.definition().m2m()), any(), token.capture());
    assertNotEquals(response.accessToken(), token.getValue());
    var bootstrap =
        new JwtConsumerBuilder()
            .setVerificationKey(pair.getPublic())
            .setExpectedIssuer("PAGOPA")
            .setExpectedAudience("api.test")
            .build()
            .processToClaims(token.getValue());
    assertEquals(30, bootstrap.getExpirationTime().getValue() - bootstrap.getIssuedAt().getValue());
  }

  @Test
  void rejectsUnconfiguredScopeBeforeSigningOrContactingIam() {
    assertThrows(ForbiddenException.class, () -> service.issue(request("admin")));
    verifyNoInteractions(service.keys, service.iam);
  }

  @Test
  void disabledIssuerDoesNotResolveOrSign() {
    when(service.config.enabled()).thenReturn(false);
    assertThrows(ResourceNotFoundException.class, () -> service.issue(request("user.read")));
    verifyNoInteractions(service.keys, service.iam);
  }

  @Test
  void failsClosedWhenIamDeniesOrIsUnavailable() {
    doThrow(new ForbiddenException("Denied"))
        .when(service.iam)
        .requirePermission(any(), any(), any(), any());
    assertThrows(ForbiddenException.class, () -> service.issue(request("user.read")));
    doThrow(new ServiceUnavailableException())
        .when(service.iam)
        .requirePermission(any(), any(), any(), any());
    assertThrows(ServiceUnavailableException.class, () -> service.issue(request("user.read")));
  }

  @Test
  void rejectsMissingKeyAndInvalidTenantConfigurationAtStartup() {
    when(service.keys.getSigningKey("PNPG")).thenThrow(new InternalException("Missing key"));
    assertThrows(InternalException.class, service::initialize);
    when(service.tenants.machineTenants())
        .thenReturn(
            List.of(
                tenant(
                    new MachineDefinition(
                        true, UID, "https://iam.test", List.of(), 3600, List.of()))));
    assertThrows(IllegalStateException.class, service::initialize);
    when(service.tenants.machineTenants())
        .thenReturn(
            List.of(
                tenant(
                    new MachineDefinition(
                        true, UID, "http://iam.test", List.of(), 300, List.of()))));
    assertThrows(IllegalStateException.class, service::initialize);
    var duplicate = new MachineGrant("user-ms", "user.read", "prod-test");
    when(service.tenants.machineTenants())
        .thenReturn(
            List.of(
                tenant(
                    new MachineDefinition(
                        true,
                        UID,
                        "https://iam.test",
                        List.of(),
                        300,
                        List.of(duplicate, duplicate)))));
    assertThrows(IllegalStateException.class, service::initialize);
  }

  private TenantRegistry.Tenant tenant(MachineDefinition machine) {
    return new TenantRegistry.Tenant(
        "PNPG", new TenantDefinition(null, null, List.of(), null, false, null, machine));
  }

  private MachineTokenRequest request(String scope) {
    return new MachineTokenRequest("PNPG", "user-ms", scope);
  }
}
