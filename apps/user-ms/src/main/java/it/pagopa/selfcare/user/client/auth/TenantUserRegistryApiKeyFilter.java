package it.pagopa.selfcare.user.client.auth;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

@ApplicationScoped
public class TenantUserRegistryApiKeyFilter implements ClientRequestFilter {
  @Inject TenantContext tenantContext;
  @Inject TenantRegistry tenantRegistry;

  @Override
  public void filter(ClientRequestContext context) {
    context.getHeaders().putSingle("x-api-key",
        tenantRegistry.userRegistryApiKey(tenantContext.requiredTenantId()));
  }
}
