package it.pagopa.selfcare.user.client.auth;

import it.pagopa.selfcare.user.conf.TenantNotificationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

@ApplicationScoped
public class TenantOneMailApiKeyFilter implements ClientRequestFilter {
  @Inject TenantNotificationConfig config;

  @Override
  public void filter(ClientRequestContext context) {
    String key = config.current().apiKey();
    if (key.isBlank()) {
      throw new IllegalStateException("Missing OneMail API key for the current tenant");
    }
    context.getHeaders().putSingle("x-api-key", key);
  }
}
