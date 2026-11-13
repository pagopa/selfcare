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
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MachineTokenServiceTest {
  static final String UID = "22222222-2222-4222-8222-222222222222";
  static final String UID_ENV = "TEST_PNPG_M2M_IAM_UID";
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
    service.runtimeConfig = mock(Config.class);
    when(service.runtimeConfig.getOptionalValue(UID_ENV, String.class))
        .thenReturn(Optional.of(UID));
    tenant =
        tenant(
            new MachineDefinition(
                true,
                UID_ENV,
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
        .requirePermission(
            eq("PNPG"), eq(UID), eq(tenant.definition().m2m()), any(), token.capture());
    assertNotEquals(response.accessToken(), token.getValue());
    var bootstrap =
        new JwtConsumerBuilder()
            .setVerificationKey(pair.getPublic())
            .setExpectedIssuer("PAGOPA")
            .setExpectedAudience("api.test")
            .build()
            .processToClaims(token.getValue());
    assertEquals(30, bootstrap.getExpirationTime().getValue() - bootstrap.getIssuedAt().getValue());
    assertEquals(UID, bootstrap.getSubject());
  }

  @Test
  void rejectsUnconfiguredScopeBeforeSigningOrContactingIam() {
    assertThrows(ForbiddenException.class, () -> service.issue(request("admin")));
    verifyNoInteractions(service.keys, service.iam);
  }

  @Test
  void disabledIssuerDoesNotResolveOrSign() {
    when(service.config.enabled()).thenReturn(false);
    service.initialize();
    assertThrows(ResourceNotFoundException.class, () -> service.issue(request("user.read")));
    verifyNoInteractions(service.keys, service.iam, service.runtimeConfig);
  }

  @Test
  void failsClosedWhenIamDeniesOrIsUnavailable() {
    doThrow(new ForbiddenException("Denied"))
        .when(service.iam)
        .requirePermission(any(), any(), any(), any(), any());
    assertThrows(ForbiddenException.class, () -> service.issue(request("user.read")));
    doThrow(new ServiceUnavailableException())
        .when(service.iam)
        .requirePermission(any(), any(), any(), any(), any());
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
                        true, UID_ENV, "https://iam.test", List.of(), 3600, List.of()))));
    assertThrows(IllegalStateException.class, service::initialize);
    when(service.tenants.machineTenants())
        .thenReturn(
            List.of(
                tenant(
                    new MachineDefinition(
                        true, UID_ENV, "http://iam.test", List.of(), 300, List.of()))));
    assertThrows(IllegalStateException.class, service::initialize);
    var duplicate = new MachineGrant("user-ms", "user.read", "prod-test");
    when(service.tenants.machineTenants())
        .thenReturn(
            List.of(
                tenant(
                    new MachineDefinition(
                        true,
                        UID_ENV,
                        "https://iam.test",
                        List.of(),
                        300,
                        List.of(duplicate, duplicate)))));
    assertThrows(IllegalStateException.class, service::initialize);
  }

  @Test
  void rejectsMissingUidReferenceAndDoesNotInterpretLiteralUidAsIdentity() {
    for (String reference : new String[] {null, "", UID}) {
      when(service.tenants.machineTenants())
          .thenReturn(
              List.of(
                  tenant(
                      new MachineDefinition(
                          true, reference, "https://iam.test", List.of(), 300, List.of()))));
      assertThrows(IllegalStateException.class, service::initialize);
    }
    verifyNoInteractions(service.keys, service.iam);
  }

  @Test
  void missingOrInvalidUidSecretFailsClosedWithoutExposingItsValue() {
    List<Optional<String>> values =
        List.of(Optional.empty(), Optional.of(" "), Optional.of("not-a-valid-uid"));
    for (Optional<String> value : values) {
      when(service.runtimeConfig.getOptionalValue(UID_ENV, String.class)).thenReturn(value);
      var failure = assertThrows(IllegalStateException.class, service::initialize);
      assertNull(failure.getCause());
      assertFalse(failure.getMessage().contains("not-a-valid-uid"));
      assertThrows(IllegalStateException.class, () -> service.issue(request("user.read")));
    }
    verifyNoInteractions(service.keys, service.iam);
  }

  @Test
  void resolvesDifferentTenantSecretsWithoutCrossTenantFallback() throws Exception {
    String arUid = "11111111-1111-4111-8111-111111111111";
    var arMachine =
        new MachineDefinition(
            true,
            "TEST_AR_M2M_IAM_UID",
            "https://iam-ar.test",
            List.of(),
            300,
            List.of(new MachineGrant("user-ms", "user.read", "prod-test")));
    var arTenant =
        new TenantRegistry.Tenant(
            "AR", new TenantDefinition(null, null, List.of(), null, false, null, arMachine));
    when(service.tenants.machineTenants()).thenReturn(List.of(arTenant, tenant));
    when(service.tenants.resolveTenant("AR")).thenReturn(arTenant);
    when(service.runtimeConfig.getOptionalValue("TEST_AR_M2M_IAM_UID", String.class))
        .thenReturn(Optional.of(arUid));
    var pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    when(service.keys.getSigningKey(anyString()))
        .thenReturn(new TenantSessionKeyProvider.SigningKey(pair.getPrivate(), "test-kid"));
    service.initialize();

    var parser =
        new JwtConsumerBuilder()
            .setVerificationKey(pair.getPublic())
            .setExpectedIssuer("PAGOPA")
            .setExpectedAudience("api.test")
            .setRequireExpirationTime()
            .build();
    var ar = service.issue(new MachineTokenRequest("AR", "user-ms", "user.read"));
    var pnpg = service.issue(request("user.read"));
    assertEquals(arUid, parser.processToClaims(ar.accessToken()).getStringClaimValue("uid"));
    assertEquals(UID, parser.processToClaims(pnpg.accessToken()).getStringClaimValue("uid"));
    verify(service.iam).requirePermission(eq("AR"), eq(arUid), any(), any(), any());
    verify(service.iam).requirePermission(eq("PNPG"), eq(UID), any(), any(), any());

    when(service.runtimeConfig.getOptionalValue(UID_ENV, String.class))
        .thenReturn(Optional.empty());
    assertThrows(IllegalStateException.class, () -> service.issue(request("user.read")));
  }

  private TenantRegistry.Tenant tenant(MachineDefinition machine) {
    return new TenantRegistry.Tenant(
        "PNPG", new TenantDefinition(null, null, List.of(), null, false, null, machine));
  }

  private MachineTokenRequest request(String scope) {
    return new MachineTokenRequest("PNPG", "user-ms", scope);
  }
}
