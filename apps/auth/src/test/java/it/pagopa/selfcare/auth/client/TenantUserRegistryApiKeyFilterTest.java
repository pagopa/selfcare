package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

class TenantUserRegistryApiKeyFilterTest {

  @Test
  void selectsKeyForEveryRequestAndRejectsMissingTenant() {
    TenantContext tenantContext = new TenantContext();
    TenantRegistry registry = mock(TenantRegistry.class);
    when(registry.userRegistryApiKey("AR")).thenReturn("ar-key");
    when(registry.userRegistryApiKey("PNPG")).thenReturn("pnpg-key");
    TenantUserRegistryApiKeyFilter filter = new TenantUserRegistryApiKeyFilter();
    filter.tenantContext = tenantContext;
    filter.tenantRegistry = registry;

    tenantContext.setTenantId("AR");
    assertEquals("ar-key", headersAfterFilter(filter).getFirst("x-api-key"));
    tenantContext.setTenantId("PNPG");
    assertEquals("pnpg-key", headersAfterFilter(filter).getFirst("x-api-key"));
    tenantContext.clear();
    assertThrows(UnresolvedTenantException.class, () -> headersAfterFilter(filter));
  }

  @Test
  void replacesAnyPreviouslyConfiguredApiKey() {
    TenantContext tenantContext = new TenantContext();
    tenantContext.setTenantId("AR");
    TenantRegistry registry = mock(TenantRegistry.class);
    when(registry.userRegistryApiKey("AR")).thenReturn("ar-key");
    TenantUserRegistryApiKeyFilter filter = new TenantUserRegistryApiKeyFilter();
    filter.tenantContext = tenantContext;
    filter.tenantRegistry = registry;
    ClientRequestContext requestContext = mock(ClientRequestContext.class);
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.putSingle("x-api-key", "global-key");
    when(requestContext.getHeaders()).thenReturn(headers);

    filter.filter(requestContext);

    assertEquals("ar-key", headers.getFirst("x-api-key"));
    assertEquals(1, headers.get("x-api-key").size());
  }

  private MultivaluedMap<String, Object> headersAfterFilter(
      TenantUserRegistryApiKeyFilter filter) {
    ClientRequestContext requestContext = mock(ClientRequestContext.class);
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    when(requestContext.getHeaders()).thenReturn(headers);
    filter.filter(requestContext);
    return headers;
  }
}
