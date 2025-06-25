package it.pagopa.selfcare.auth.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

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

    String headers =
        requestContext.getStringHeaders().entrySet().stream()
            .map(
                entry ->
                    String.format(
                        "Header name:%s - value:%s",
                        entry.getKey(), String.join(",", entry.getValue())))
            .collect(Collectors.joining(";"));
    String body = requestContext.getEntity().toString();

      log.info("Request: method: {}, endpoint: {}, headers; {}, query: {}, body: {}", method, endpoint, headers, query, body);
  }
}
