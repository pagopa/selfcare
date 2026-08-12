package it.pagopa.selfcare.auth.client;

import it.pagopa.selfcare.auth.context.TokenContext;
import it.pagopa.selfcare.security.tenant.TenantConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class IamMsHeadersFactory implements ClientHeadersFactory {

  @Inject TokenContext tokenContext;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incoming, MultivaluedMap<String, String> outgoing) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    result.putAll(outgoing);
    if (tokenContext.getToken() != null) {
      result.add("Authorization", "Bearer " + tokenContext.getToken());
      // iam enforces X-Tenant-Id against the token's tenant_id claim (Step_0 SELC-5): send the
      // header only when it is the tenant that went into the token we are sending, otherwise the
      // request would be rejected for mismatch instead of just missing authorisation context.
      if (tokenContext.getTenantId() != null) {
        result.add(TenantConstants.TENANT_HEADER, tokenContext.getTenantId());
      }
    }
    return result;
  }
}
