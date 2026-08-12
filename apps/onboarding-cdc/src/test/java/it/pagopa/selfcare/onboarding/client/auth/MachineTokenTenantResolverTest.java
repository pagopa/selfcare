package it.pagopa.selfcare.onboarding.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MachineTokenTenantResolverTest {

  private static String tokenWithPayload(String json) {
    String payload =
        Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    return "header." + payload + ".signature";
  }

  @Test
  void readsTheTenantClaimFromTheMachineToken() {
    Optional<String> tenant =
        MachineTokenTenantResolver.tenantOf(tokenWithPayload("{\"sub\":\"svc\",\"tenant_id\":\"AR\"}"));

    assertEquals(Optional.of("AR"), tenant);
  }

  @Test
  void returnsEmptyWhenTheTokenCarriesNoTenantClaim() {
    // Sending a fabricated header here would be rejected downstream as a mismatch and would hide
    // the real problem, which is a machine token that must be re-issued with the claim.
    assertTrue(MachineTokenTenantResolver.tenantOf(tokenWithPayload("{\"sub\":\"svc\"}")).isEmpty());
  }

  @Test
  void returnsEmptyWhenTheTenantClaimIsBlank() {
    assertTrue(
        MachineTokenTenantResolver.tenantOf(tokenWithPayload("{\"tenant_id\":\"  \"}")).isEmpty());
  }

  @Test
  void returnsEmptyWhenTheTokenIsMissingOrMalformed() {
    assertTrue(MachineTokenTenantResolver.tenantOf(null).isEmpty());
    assertTrue(MachineTokenTenantResolver.tenantOf("").isEmpty());
    assertTrue(MachineTokenTenantResolver.tenantOf("not-a-jwt").isEmpty());
    assertTrue(MachineTokenTenantResolver.tenantOf("header.!!not-base64!!.sig").isEmpty());
  }
}
