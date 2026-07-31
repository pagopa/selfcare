package it.pagopa.selfcare.onboarding.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MachineTokenTenantResolverTest {

  @Test
  void readsTenantClaim() {
    assertEquals(
        "PNPG",
        MachineTokenTenantResolver.tenantOf(tokenWithPayload("{\"tenant_id\":\"PNPG\"}"))
            .orElseThrow());
  }

  @Test
  void rejectsMissingOrMalformedClaim() {
    assertTrue(
        MachineTokenTenantResolver.tenantOf(tokenWithPayload("{\"sub\":\"svc\"}")).isEmpty());
    assertTrue(MachineTokenTenantResolver.tenantOf("not-a-jwt").isEmpty());
    assertTrue(MachineTokenTenantResolver.tenantOf(null).isEmpty());
  }

  private static String tokenWithPayload(String json) {
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    return "header." + payload + ".signature";
  }
}
