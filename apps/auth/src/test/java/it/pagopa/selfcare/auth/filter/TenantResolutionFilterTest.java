package it.pagopa.selfcare.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class TenantResolutionFilterTest {

  private String sanitize(String value) throws Exception {
    Method method = TenantResolutionFilter.class.getDeclaredMethod("sanitizeForLog", String.class);
    method.setAccessible(true);
    return (String) method.invoke(new TenantResolutionFilter(), value);
  }

  @Test
  void sanitizeRemovesCrlfPreventingAuditLogForgery() throws Exception {
    String forged = "otp/verify\nevent=tenant_request_rejected reason=none";

    String sanitized = sanitize(forged);

    assertFalse(sanitized.contains("\n"));
    assertFalse(sanitized.contains("\r"));
    assertFalse(sanitized.contains(" "));
    assertTrue(sanitized.startsWith("otp/verify"));
  }

  @Test
  void sanitizeKeepsLegitimatePathCharacters() throws Exception {
    assertEquals("otp/verify", sanitize("otp/verify"));
    assertEquals("oidc/exchange-v2.1", sanitize("oidc/exchange-v2.1"));
  }

  @Test
  void sanitizeTruncatesOverlongPaths() throws Exception {
    String sanitized = sanitize("a".repeat(500));

    assertEquals(200, sanitized.length());
  }

  @Test
  void sanitizeHandlesNull() throws Exception {
    assertEquals("", sanitize(null));
  }
}
