package it.pagopa.selfcare.auth.client;

import it.pagopa.selfcare.auth.context.AuthTenantContext;
import it.pagopa.selfcare.auth.context.TokenContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class IamMsHeadersFactory implements ClientHeadersFactory {

  @Inject TokenContext tokenContext;
  @Inject AuthTenantContext tenantContext;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incoming, MultivaluedMap<String, String> outgoing) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    result.putAll(outgoing);
    result.putSingle("X-Tenant-Id", tenantContext.getTenantId());
    if (tokenContext.getToken() != null) {
      result.add("Authorization", "Bearer " + tokenContext.getToken());
    }
    return result;
  }
}
