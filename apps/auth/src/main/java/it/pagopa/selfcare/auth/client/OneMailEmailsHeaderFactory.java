package it.pagopa.selfcare.auth.client;

import it.pagopa.selfcare.auth.conf.TenantOutboundMailConfig;
import it.pagopa.selfcare.auth.context.AuthTenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class OneMailEmailsHeaderFactory implements ClientHeadersFactory {
  private static final String HEADER_NAME = "x-api-key";

  @Inject TenantOutboundMailConfig mailConfig;
  @Inject AuthTenantContext tenantContext;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> multivaluedMap,
      MultivaluedMap<String, String> multivaluedMap1) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.putSingle(HEADER_NAME, mailConfig.apiKey(tenantContext.getTenantId()));
    return headers;
  }
}
