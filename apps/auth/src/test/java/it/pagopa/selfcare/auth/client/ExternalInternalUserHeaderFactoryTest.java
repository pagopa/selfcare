package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.context.AuthTenantContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

class ExternalInternalUserHeaderFactoryTest {

  @Test
  void addsApiKeyAndCurrentTenantToOutgoingHeaders() {
    ExternalInternalUserHeaderFactory factory = factoryForTenant("AR");

    var headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

    assertEquals("AR", headers.getFirst("X-Tenant-Id"));
    assertEquals("subscription", headers.getFirst("Ocp-Apim-Subscription-Key"));
    assertEquals(2, headers.size());
  }

  @Test
  void doesNotPreserveStaleIncomingTenantOrSubscriptionHeaders() {
    ExternalInternalUserHeaderFactory factory = factoryForTenant("AR");
    MultivaluedMap<String, String> existingHeaders = new MultivaluedHashMap<>();
    existingHeaders.putSingle("X-Tenant-Id", "PNPG");
    existingHeaders.putSingle("Ocp-Apim-Subscription-Key", "stale-key");

    var headers = factory.update(existingHeaders, existingHeaders);

    assertEquals("AR", headers.getFirst("X-Tenant-Id"));
    assertEquals("subscription", headers.getFirst("Ocp-Apim-Subscription-Key"));
    assertEquals(2, headers.size());
  }

  @Test
  void failsWithoutTenantInsteadOfSendingSubscriptionKey() {
    AuthTenantContext tenantContext = mock(AuthTenantContext.class);
    when(tenantContext.getTenantId()).thenThrow(new IllegalStateException("No tenant"));
    ExternalInternalUserHeaderFactory factory = new ExternalInternalUserHeaderFactory();
    factory.apiKey = "subscription";
    factory.tenantContext = tenantContext;

    assertThrows(
        IllegalStateException.class,
        () -> factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>()));
  }

  private ExternalInternalUserHeaderFactory factoryForTenant(String tenantId) {
    AuthTenantContext tenantContext = mock(AuthTenantContext.class);
    when(tenantContext.getTenantId()).thenReturn(tenantId);
    ExternalInternalUserHeaderFactory factory = new ExternalInternalUserHeaderFactory();
    factory.apiKey = "subscription";
    factory.tenantContext = tenantContext;
    return factory;
  }
}
