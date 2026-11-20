package it.pagopa.selfcare.institution.client.auth;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationPropagationHeadersFactoryTest {
  @Test void propagatesConfiguredTenantAndReplacesUntrustedHeader() {
    var factory = new AuthenticationPropagationHeadersFactory();
    factory.tenantId = "AR";
    var headers = new MultivaluedHashMap<String, String>();
    headers.putSingle("X-Tenant-Id", "PNPG");
    factory.update(new MultivaluedHashMap<>(), headers);
    assertEquals("AR", headers.getFirst("X-Tenant-Id"));
    assertEquals(1, headers.get("X-Tenant-Id").size());
    assertTrue(headers.containsKey("Authorization"));
  }

  @Test void rejectsMissingTenantConfiguration() {
    var factory = new AuthenticationPropagationHeadersFactory();
    factory.tenantId = " ";
    assertThrows(IllegalStateException.class,
        () -> factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>()));
  }
}
