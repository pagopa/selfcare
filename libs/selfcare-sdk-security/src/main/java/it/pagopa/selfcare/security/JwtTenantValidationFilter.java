package it.pagopa.selfcare.security;

import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.tenant.TenantContext;
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
  @Inject TenantContext tenantContext;

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
      tenantContext.setTenantId(tenantId);
    } catch (TenantValidationException exception) {
      // An invalid/mismatched tenant means the JWT cannot be trusted for this request, which is
      // an authentication failure (401), not a client request-formation error (400).
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .type("application/problem+json")
              .entity(
                  Map.of(
                      "title", Response.Status.UNAUTHORIZED.getReasonPhrase(),
                      "status", Response.Status.UNAUTHORIZED.getStatusCode(),
                      "detail", exception.getMessage()))
              .build());
    }
  }
}
