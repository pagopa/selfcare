package it.pagopa.selfcare.iam.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

@Provider
@Slf4j
public class CustomClientRequestLoggingFilter implements ResteasyReactiveClientRequestFilter {

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    ResteasyReactiveClientRequestFilter.super.filter(requestContext);
  }

  @Override
  public void filter(ResteasyReactiveClientRequestContext requestContext) {
    String endpoint = requestContext.getUri().getPath();
    String query = requestContext.getUri().getQuery();
    String method = requestContext.getMethod();
    MDCUtils.addOperationIdAndParameters(method);
    log.info("Request: method: {}, endpoint: {}, query: {}", method, endpoint, query);
  }
}
