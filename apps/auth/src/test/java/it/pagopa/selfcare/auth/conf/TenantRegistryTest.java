package it.pagopa.selfcare.auth.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantRegistryTest {

  @Inject TenantRegistry tenantRegistry;

  @Inject it.pagopa.selfcare.tenant.TenantRegistry resourceRegistry;

  @Test
  void authenticationAndResourceRegistriesReadTheSameTenantDefinition() {
    assertEquals("AR", tenantRegistry.resolveEnabledTenant("AR").id());
    assertEquals("selcAuth", resourceRegistry.resolve("AR").mongo().database());
    assertEquals("id", tenantRegistry.oneIdentityCredentials("AR").clientId());
    assertEquals("123", resourceRegistry.userRegistryApiKey("AR"));
    assertNull(resourceRegistry.resolve("PNPG").mongo());
  }

  @Test
  void resolveEnabledTenantReturnsAr() {
    assertEquals("AR", tenantRegistry.resolveEnabledTenant("AR").id());
  }

  @Test
  void resolveEnabledTenantRejectsDisabledTenant() {
    assertThrows(ForbiddenException.class, () -> tenantRegistry.resolveEnabledTenant("PNPG"));
  }

  @Test
  void resolveEnabledTenantRejectsUnknownTenant() {
    assertThrows(InvalidRequestException.class, () -> tenantRegistry.resolveEnabledTenant("OTHER"));
  }

  @Test
  void resolveEnabledTenantRejectsMissingHeader() {
    assertThrows(InvalidRequestException.class, () -> tenantRegistry.resolveEnabledTenant(null));
  }

  @Test
  void resolveEnabledTenantRejectsBlankHeader() {
    assertThrows(InvalidRequestException.class, () -> tenantRegistry.resolveEnabledTenant(" "));
  }

  @Test
  void oneIdentityCredentialsAreTenantSpecific() {
    TenantRegistry.OneIdentityCredentials credentials = tenantRegistry.oneIdentityCredentials("AR");

    assertEquals("id", credentials.clientId());
    assertEquals("secret", credentials.clientSecret());
  }

  @Test
  void enabledAuthenticationTenantsContainOnlyEnabledTenants() {
    assertEquals(
        java.util.List.of("AR"),
        tenantRegistry.enabledAuthenticationTenants().stream()
            .map(TenantRegistry.Tenant::id)
            .toList());
  }

  @Test
  void oneIdentityCredentialsRejectTenantUsingAnotherProvider() {
    assertThrows(
        ForbiddenException.class, () -> tenantRegistry.oneIdentityCredentials("PNPG"));
  }

  @Test
  void initializeRejectsInvalidRegistryJson() {
    TenantRegistry registry = registry("{invalid", mock(it.pagopa.selfcare.tenant.TenantRegistry.class));

    assertThrows(IllegalStateException.class, registry::initialize);
  }

  @Test
  void oneIdentityCredentialsRejectMissingSharedConfiguration() {
    it.pagopa.selfcare.tenant.TenantRegistry resources =
        mock(it.pagopa.selfcare.tenant.TenantRegistry.class);
    when(resources.oneIdentityCredentials("AR")).thenReturn(Optional.empty());
    TenantRegistry registry = registry(enabledOneIdentityTenantJson(), resources);
    registry.initialize();

    assertThrows(IllegalStateException.class, () -> registry.oneIdentityCredentials("AR"));
  }

  @Test
  void oneIdentityCredentialsRejectInvalidSharedConfiguration() {
    it.pagopa.selfcare.tenant.TenantRegistry resources =
        mock(it.pagopa.selfcare.tenant.TenantRegistry.class);
    when(resources.oneIdentityCredentials("AR"))
        .thenThrow(new IllegalStateException("Missing OneIdentity configuration for tenant AR"));
    TenantRegistry registry = registry(enabledOneIdentityTenantJson(), resources);
    registry.initialize();

    assertThrows(IllegalStateException.class, () -> registry.oneIdentityCredentials("AR"));
  }

  @Test
  void initializeRequiresCredentialsOnlyForEnabledOneIdentityTenants() {
    it.pagopa.selfcare.tenant.TenantRegistry resources =
        mock(it.pagopa.selfcare.tenant.TenantRegistry.class);
    when(resources.oneIdentityCredentials("AR"))
        .thenReturn(
            Optional.of(new it.pagopa.selfcare.tenant.TenantRegistry.OneIdentityCredentials(
                "ar-client", "ar-secret")));
    TenantRegistry registry =
        registry(
            """
            {
              "AR": {
                "authentication_provider": "ONE_IDENTITY",
                "auth_enabled": true
              },
              "PNPG": {
                "authentication_provider": "HUB_SPID_LOGIN",
                "auth_enabled": true
              },
              "DISABLED": {
                "authentication_provider": "ONE_IDENTITY",
                "auth_enabled": false
              }
            }
            """,
            resources);

    registry.initialize();

    assertEquals(
        java.util.List.of("AR", "PNPG"),
        registry.enabledAuthenticationTenants().stream()
            .map(TenantRegistry.Tenant::id)
            .sorted()
            .toList());
    assertEquals("ar-client", registry.oneIdentityCredentials("AR").clientId());
    assertThrows(ForbiddenException.class, () -> registry.oneIdentityCredentials("PNPG"));
    assertThrows(ForbiddenException.class, () -> registry.oneIdentityCredentials("DISABLED"));
  }

  private TenantRegistry registry(
      String registryJson, it.pagopa.selfcare.tenant.TenantRegistry resources) {
    TenantRegistry registry = new TenantRegistry();
    registry.objectMapper = new ObjectMapper();
    registry.resourceRegistry = resources;
    registry.tenantRegistryJson = registryJson;
    return registry;
  }

  private String enabledOneIdentityTenantJson() {
    return """
        {
          "AR": {
            "authentication_provider": "ONE_IDENTITY",
            "auth_enabled": true
          }
        }
        """;
  }
}
