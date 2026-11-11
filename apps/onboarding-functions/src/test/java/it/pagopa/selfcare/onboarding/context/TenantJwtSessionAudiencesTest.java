package it.pagopa.selfcare.onboarding.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantJwtSessionAudiencesTest {

  @Inject TenantJwtSessionAudiences audiences;
  @Inject TenantRegistry registry;

  @Test
  void readsAudienceOfEachTenantFromRegistry() {
    assertEquals("api.dev.selfcare.pagopa.it", audiences.audience("AR"));
    assertEquals("pnpg.dev.selfcare.pagopa.it", audiences.audience("pnpg"));
  }

  @Test
  void pnpgAudienceKeepsInstitutionMsTaxCodeRule() {
    // institution-ms skips the tax code encryption when the audience contains one of these values
    String audience = audiences.audience("PNPG");
    assertTrue(audience.contains("pnpg") || audience.contains("imprese.notifichedigitali"));
  }

  @Test
  void rejectsUnknownTenant() {
    assertThrows(IllegalArgumentException.class, () -> audiences.audience("UNKNOWN"));
  }

  @Test
  void rejectsTenantWithoutAudienceWithoutFallback() {
    TenantJwtSessionAudiences arOnly =
        new TenantJwtSessionAudiences(
            registry, "{\"AR\":{\"jwt\":{\"session\":{\"audience\":\"api.dev.selfcare.pagopa.it\"}}}}");

    assertThrows(IllegalStateException.class, () -> arOnly.audience("PNPG"));
  }

  @Test
  void rejectsInvalidRegistry() {
    assertThrows(IllegalStateException.class, () -> new TenantJwtSessionAudiences(registry, "{"));
  }
}
