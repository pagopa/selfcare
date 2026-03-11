package it.pagopa.selfcare.iam.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter; // Importiamo l'interfaccia standard
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Slf4j
@Provider
@ApplicationScoped
public class TokenPropagationFilter implements ClientRequestFilter {
  @Inject JsonWebToken jwt;

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    try {
      if (jwt != null && jwt.getRawToken() != null) {
        requestContext
            .getHeaders()
            .putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getRawToken());
      } else {
        log.warn(
            "No JWT token found in the current context. The request will proceed without an Authorization header.");
      }
    } catch (Exception e) {
      log.error("Error fetching JWT token: {}", e.getMessage());
    }
  }
}
