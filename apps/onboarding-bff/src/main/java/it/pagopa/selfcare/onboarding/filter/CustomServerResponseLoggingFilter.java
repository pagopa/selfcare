package it.pagopa.selfcare.onboarding.filter;

import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerResponseFilter;
import org.slf4j.MDC;

@Provider
public class CustomServerResponseLoggingFilter implements ResteasyReactiveContainerResponseFilter {

  @Override
  public void filter(
      ResteasyReactiveContainerRequestContext requestContext,
      ContainerResponseContext responseContext) {
    MDC.remove("X-Client-Ip");
    MDC.remove("sc_operation_id");
  }
}
