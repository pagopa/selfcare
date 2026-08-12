package it.pagopa.selfcare.auth.filter;

import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.context.AuthTenantContext;
import it.pagopa.selfcare.auth.controller.response.Problem;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class TenantResolutionFilter implements ContainerRequestFilter {

  public static final String TENANT_HEADER = "X-Tenant-Id";

  @Inject TenantRegistry tenantRegistry;
  @Inject AuthTenantContext tenantContext;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!requiresTenant(requestContext.getUriInfo().getPath())) {
      return;
    }

    try {
      tenantContext.setTenant(
          tenantRegistry.resolveEnabledTenant(requestContext.getHeaderString(TENANT_HEADER)));
    } catch (InvalidRequestException exception) {
      requestContext.abortWith(problem(Response.Status.BAD_REQUEST, exception.getMessage()));
    } catch (ForbiddenException exception) {
      requestContext.abortWith(problem(Response.Status.FORBIDDEN, exception.getMessage()));
    }
  }

  private boolean requiresTenant(String path) {
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    return normalizedPath.equals("oidc")
        || normalizedPath.startsWith("oidc/")
        || normalizedPath.equals("otp")
        || normalizedPath.startsWith("otp/")
        || normalizedPath.equals("saml")
        || normalizedPath.startsWith("saml/");
  }

  private Response problem(Response.Status status, String message) {
    return Response.status(status)
        .type("application/problem+json")
        .entity(
            Problem.builder()
                .title(status.getReasonPhrase())
                .status(status.getStatusCode())
                .detail(message)
                .build())
        .build();
  }
}
