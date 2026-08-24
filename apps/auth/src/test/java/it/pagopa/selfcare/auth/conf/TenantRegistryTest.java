package it.pagopa.selfcare.auth.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantRegistryTest {

  @Inject TenantRegistry tenantRegistry;

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
  void oneIdentityCredentialsAreTenantSpecific() {
    TenantRegistry.OneIdentityCredentials credentials = tenantRegistry.oneIdentityCredentials("AR");

    assertEquals("id", credentials.clientId());
    assertEquals("secret", credentials.clientSecret());
  }
}
