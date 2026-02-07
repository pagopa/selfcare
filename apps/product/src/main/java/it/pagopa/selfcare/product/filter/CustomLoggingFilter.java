package it.pagopa.selfcare.product.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.owasp.encoder.Encode;

@Slf4j
@Provider
public class CustomLoggingFilter implements ResteasyReactiveContainerRequestFilter {

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    ResteasyReactiveContainerRequestFilter.super.filter(requestContext);
  }

  @Override
  public void filter(ResteasyReactiveContainerRequestContext requestContext) {
    String endpoint = requestContext.getUriInfo().getPath();
    String method = requestContext.getMethod();
    log.info("Request: method: {}, endpoint: {}", Encode.forJava(method), Encode.forJava(endpoint));
  }
}
