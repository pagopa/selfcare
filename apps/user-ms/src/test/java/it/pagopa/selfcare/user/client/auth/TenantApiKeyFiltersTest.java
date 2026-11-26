package it.pagopa.selfcare.user.client.auth;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TenantApiKeyFiltersTest {
  @Inject TenantContext context;
  @Inject TenantUserRegistryApiKeyFilter registryFilter;

  @Test void overridesUserRegistryApiKeyWithCurrentTenantCredentials() {
    ClientRequestContext request = mock(ClientRequestContext.class);
    var headers = new MultivaluedHashMap<String, Object>();
    when(request.getHeaders()).thenReturn(headers);
    for (String tenant : new String[]{"AR", "PNPG", "AR"}) {
      context.setTenantId(tenant);
      registryFilter.filter(request);
      assertEquals(tenant.toLowerCase() + "-test-key", headers.getFirst("x-api-key"));
      assertEquals(1, headers.get("x-api-key").size());
    }
    context.clear();
    assertThrows(UnresolvedTenantException.class, () -> registryFilter.filter(request));
  }
}
