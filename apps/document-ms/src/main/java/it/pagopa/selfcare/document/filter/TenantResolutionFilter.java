package it.pagopa.selfcare.document.filter;

import it.pagopa.selfcare.document.model.dto.response.Problem;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves the tenant of every business request from the {@code X-Tenant-Id} header and stores it
 * in the request-scoped {@link TenantContext}. Requests without a valid, unique tenant are rejected
 * (fail-closed) before reaching controllers.
 */
@Slf4j
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TenantResolutionFilter implements ContainerRequestFilter {

  public static final String TENANT_HEADER = "X-Tenant-Id";
  public static final String INVALID_TENANT_CONTEXT = "Invalid tenant context";

  @Inject TenantRegistry tenantRegistry;

  @Inject TenantContext tenantContext;

  @ConfigProperty(name = "tenant.enforcement.enabled", defaultValue = "true")
  boolean tenantEnforcementEnabled;

  @ConfigProperty(name = "tenant.default", defaultValue = "AR")
  String defaultTenant;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (isTechnicalPath(requestContext.getUriInfo().getPath())) {
      return;
    }

    List<String> headerValues = requestContext.getHeaders().get(TENANT_HEADER);
    try {
      String tenant = selectTenant(headerValues);
      tenantRegistry.resolve(tenant);
      tenantContext.setTenantId(tenantRegistry.normalizeTenantId(tenant));
    } catch (RuntimeException exception) {
      log.warn(
          "Rejected request with invalid tenant context: tenant={}",
          TenantLogUtils.sanitize(headerValues == null ? null : String.join(",", headerValues)));
      requestContext.abortWith(invalidTenantResponse());
    }
  }

  private String selectTenant(List<String> headerValues) {
    if (headerValues != null && headerValues.size() > 1) {
      throw new IllegalArgumentException("Multiple tenant headers");
    }
    String headerTenant =
        headerValues == null || headerValues.isEmpty() ? null : headerValues.get(0);
    if (headerTenant != null && headerTenant.contains(",")) {
      throw new IllegalArgumentException("Multiple tenant values");
    }
    if (!tenantEnforcementEnabled && (headerTenant == null || headerTenant.isBlank())) {
      return defaultTenant;
    }
    return headerTenant;
  }

  static boolean isTechnicalPath(String path) {
    String normalized = path == null ? "" : path.replaceFirst("^/+", "");
    return normalized.equals("q") || normalized.startsWith("q/");
  }

  static Response invalidTenantResponse() {
    Problem problem =
        Problem.builder()
            .title(INVALID_TENANT_CONTEXT)
            .detail(INVALID_TENANT_CONTEXT)
            .status(Response.Status.BAD_REQUEST.getStatusCode())
            .build();
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(problem)
        .type("application/problem+json")
        .build();
  }
}
