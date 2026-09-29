package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.context.AuthTenantContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

class TenantPropagationHeadersTest {

  @Test
  void externalInternalUserCallsCarryValidatedTenant() {
    AuthTenantContext tenantContext = mock(AuthTenantContext.class);
    when(tenantContext.getTenantId()).thenReturn("AR");
    ExternalInternalUserHeaderFactory factory = new ExternalInternalUserHeaderFactory();
    factory.apiKey = "subscription";
    factory.tenantContext = tenantContext;

    var headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    assertEquals("AR", headers.getFirst("X-Tenant-Id"));
    assertEquals("subscription", headers.getFirst("Ocp-Apim-Subscription-Key"));
  }

  @Test
  void internalUserCallsCarryValidatedTenant() {
    AuthTenantContext tenantContext = mock(AuthTenantContext.class);
    when(tenantContext.getTenantId()).thenReturn("AR");
    InternalUserMsHeaderFactory factory = new InternalUserMsHeaderFactory();
    factory.apiKey = "subscription";
    factory.tenantContext = tenantContext;

    var headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    assertEquals("AR", headers.getFirst("X-Tenant-Id"));
    assertEquals("subscription", headers.getFirst("Ocp-Apim-Subscription-Key"));
  }

  @Test
  void missingTenantNeverSendsASubscriptionKey() {
    AuthTenantContext tenantContext = mock(AuthTenantContext.class);
    when(tenantContext.getTenantId()).thenThrow(new IllegalStateException("No tenant"));
    ExternalInternalUserHeaderFactory factory = new ExternalInternalUserHeaderFactory();
    factory.apiKey = "subscription";
    factory.tenantContext = tenantContext;

    assertThrows(IllegalStateException.class,
        () -> factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>()));
  }
}
