package it.pagopa.selfcare.iam.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantRegistryTest {

  @Inject TenantRegistry tenantRegistry;

  @Test
  void resolveTenantReturnsAr() {
    assertEquals("AR", tenantRegistry.resolveTenant("AR").id());
  }

  @Test
  void resolveTenantReturnsPnpg() {
    assertEquals("PNPG", tenantRegistry.resolveTenant("PNPG").id());
  }

  @Test
  void resolveTenantRejectsUnknownTenant() {
    assertThrows(InvalidRequestException.class, () -> tenantRegistry.resolveTenant("OTHER"));
  }

  @Test
  void resolveTenantRejectsMissingHeader() {
    assertThrows(InvalidRequestException.class, () -> tenantRegistry.resolveTenant(null));
  }

  @Test
  void resolveTenantRejectsBlankHeader() {
    assertThrows(InvalidRequestException.class, () -> tenantRegistry.resolveTenant("  "));
  }
}
