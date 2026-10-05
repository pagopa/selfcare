package it.pagopa.selfcare.document.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

/** Removes the tenant from the logging MDC once the response is produced. */
@Provider
public class TenantMdcCleanupFilter implements ContainerResponseFilter {

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.remove(TenantResolutionFilter.TENANT_MDC_KEY);
  }
}
