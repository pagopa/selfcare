package it.pagopa.selfcare.onboarding.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Objects;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.owasp.encoder.Encode;
import org.slf4j.MDC;

@Provider
public class CustomLoggingFilter implements ResteasyReactiveContainerRequestFilter {

  private static final Logger LOG = Logger.getLogger(CustomLoggingFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    ResteasyReactiveContainerRequestFilter.super.filter(requestContext);
  }

  @Override
  public void filter(ResteasyReactiveContainerRequestContext requestContext) {
    String clientIp = requestContext.getHeaders().getFirst("X-Client-Ip");
    if (Objects.nonNull(clientIp)) {
      MDC.put("X-Client-Ip", Encode.forJava(clientIp));
    }
    String endpoint = requestContext.getUriInfo().getPath();
    String method = requestContext.getMethod();
    LOG.infof(
        "Request: method: %s, endpoint: %s", Encode.forJava(method), Encode.forJava(endpoint));
  }
}
