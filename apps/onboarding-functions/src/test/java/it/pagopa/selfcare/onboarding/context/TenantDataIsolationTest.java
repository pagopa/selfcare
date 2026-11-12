package it.pagopa.selfcare.onboarding.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Arrays;
import org.bson.Document;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantDataIsolationTest {

  private final TenantDataIsolation lenient = new TenantDataIsolation(false);
  private final TenantDataIsolation strict = new TenantDataIsolation(true);

  @Test
  void tenantFilterIncludesLegacyDocumentsOnlyWithoutStrictMode() {
    try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
      assertEquals(
          new Document("tenantId", new Document("$in", Arrays.asList("PNPG", null))),
          lenient.tenantFilter());
      assertEquals(new Document("tenantId", "PNPG"), strict.tenantFilter());
    }
  }

  @Test
  void missingTenantBelongsToCurrentTenantOnlyWithoutStrictMode() {
    try (TenantContext.Scope ignored = TenantContext.open("AR")) {
      assertEquals("AR", lenient.ownerTenant(null));
      assertEquals("AR", lenient.ownerTenant(" "));
      assertThrows(IllegalArgumentException.class, () -> strict.ownerTenant(null));
    }
  }

  @Test
  void missingTenantWithoutCurrentTenantIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> lenient.ownerTenant(null));
  }

  @Test
  void documentTenantIsResolvedAgainstRegistry() {
    assertEquals("PNPG", strict.ownerTenant("pnpg"));
    assertThrows(IllegalArgumentException.class, () -> lenient.ownerTenant("UNKNOWN"));
  }
}
