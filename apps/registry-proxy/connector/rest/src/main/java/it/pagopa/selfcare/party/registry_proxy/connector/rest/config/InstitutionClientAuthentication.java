package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

// Only the institution Feign child context must import this configuration.
public class InstitutionClientAuthentication {

  @Bean
  public AuthorizationHeaderInterceptor institutionAuthentication(
      ObjectProvider<TenantContext> tenantContext) {
    return new AuthorizationHeaderInterceptor(tenantContext);
  }
}
