package it.pagopa.selfcare.auth.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class InternalUserMsHeaderFactory implements ClientHeadersFactory {
  private static final String HEADER_NAME = "Ocp-Apim-Subscription-Key";

  @Inject
  @ConfigProperty(name = "internal.user-ms.api.key")
  String apiKey;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> multivaluedMap,
      MultivaluedMap<String, String> multivaluedMap1) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.putSingle(HEADER_NAME, apiKey);
    return headers;
  }
}
