package it.pagopa.selfcare.iam.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;
import org.slf4j.MDC;

@Provider
@Slf4j
public class CustomClientResponseLoggingFilter implements ResteasyReactiveClientResponseFilter {

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
    ResteasyReactiveClientResponseFilter.super.filter(requestContext, responseContext);
  }

  @Override
  public void filter(
      ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
    String endpoint = requestContext.getUri().getPath();
    String method = requestContext.getMethod();
    int status = responseContext.getStatus();
    log.info("event=outbound_response method={} endpoint={} status={}", method, endpoint, status);
    MDC.clear();
  }
}
