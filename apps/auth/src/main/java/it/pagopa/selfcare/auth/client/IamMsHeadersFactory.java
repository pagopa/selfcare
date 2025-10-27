package it.pagopa.selfcare.auth.client;

import it.pagopa.selfcare.auth.context.TokenContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class IamMsHeadersFactory implements ClientHeadersFactory {

  @Inject
  TokenContext tokenContext;

  @Override
  public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incoming,
                                               MultivaluedMap<String, String> outgoing) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    result.putAll(outgoing);
    if (tokenContext.getToken() != null) {
      result.add("Authorization", "Bearer " + tokenContext.getToken());
    }
    return result;
  }
}