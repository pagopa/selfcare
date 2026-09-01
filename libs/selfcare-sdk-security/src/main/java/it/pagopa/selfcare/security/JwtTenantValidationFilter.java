package it.pagopa.selfcare.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHORIZATION)
public class JwtTenantValidationFilter implements ContainerRequestFilter {

  @Inject SecurityIdentity securityIdentity;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!(securityIdentity.getPrincipal() instanceof JsonWebToken jwt)
        || !"SPID".equals(jwt.getIssuer())) {
      return;
    }

    try {
      String tenantId = JwtTenantValidator.resolveTokenTenant(jwt);
      JwtTenantValidator.validateHeader(
          tenantId, requestContext.getHeaderString(JwtTenantValidator.TENANT_HEADER));
    } catch (TenantValidationException exception) {
      requestContext.abortWith(
          Response.status(Response.Status.BAD_REQUEST)
              .type("application/problem+json")
              .entity(
                  Map.of(
                      "title", Response.Status.BAD_REQUEST.getReasonPhrase(),
                      "status", Response.Status.BAD_REQUEST.getStatusCode(),
                      "detail", exception.getMessage()))
              .build());
    }
  }
}
