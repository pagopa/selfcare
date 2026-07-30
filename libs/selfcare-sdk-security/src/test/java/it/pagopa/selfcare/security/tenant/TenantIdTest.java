package it.pagopa.selfcare.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantIdTest {

  @Test
  void shouldResolveKnownTenantsCaseInsensitively() {
    assertEquals(Optional.of(TenantId.AR), TenantId.fromValue("AR"));
    assertEquals(Optional.of(TenantId.AR), TenantId.fromValue("ar"));
    assertEquals(Optional.of(TenantId.PNPG), TenantId.fromValue("PNPG"));
    assertEquals(Optional.of(TenantId.PNPG), TenantId.fromValue("pnpg"));
  }

  @Test
  void shouldTrimSurroundingWhitespace() {
    assertEquals(Optional.of(TenantId.AR), TenantId.fromValue("  AR  "));
  }

  @Test
  void shouldReturnEmptyForUnknownOrBlankValues() {
    assertTrue(TenantId.fromValue("UNKNOWN").isEmpty());
    assertTrue(TenantId.fromValue("").isEmpty());
    assertTrue(TenantId.fromValue("   ").isEmpty());
    assertTrue(TenantId.fromValue(null).isEmpty());
  }
}
