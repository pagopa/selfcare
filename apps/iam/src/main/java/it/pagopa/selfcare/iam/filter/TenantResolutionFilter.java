package it.pagopa.selfcare.iam.filter;

import it.pagopa.selfcare.iam.controller.response.Problem;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the tenant of the request from the {@code tenantId} query parameter or the {@code
 * X-Tenant-Id} header and stores it in {@link TenantContext}. Conflicting or unknown tenants are
 * rejected with 400. When {@code tenant.enforcement.enabled} is false, a request without tenant
 * falls back to {@code tenant.default}.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class TenantResolutionFilter implements ContainerRequestFilter {

  public static final String TENANT_HEADER = "X-Tenant-Id";
  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_TENANT");
  private static final int MAX_LOGGED_PATH_LENGTH = 200;

  private final TenantRegistry tenantRegistry;
  private final TenantContext tenantContext;
  private final boolean tenantEnforcementEnabled;
  private final String defaultTenant;

  @Inject
  public TenantResolutionFilter(
      TenantRegistry tenantRegistry,
      TenantContext tenantContext,
      @ConfigProperty(name = "tenant.enforcement.enabled", defaultValue = "true")
          boolean tenantEnforcementEnabled,
      @ConfigProperty(name = "tenant.default", defaultValue = "AR") String defaultTenant) {
    this.tenantRegistry = tenantRegistry;
    this.tenantContext = tenantContext;
    this.tenantEnforcementEnabled = tenantEnforcementEnabled;
    this.defaultTenant = defaultTenant;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    if (normalizedPath.startsWith("q/") || normalizedPath.equals("q")) {
      return;
    }

    Optional<String> queryTenant = tenantFromQuery(requestContext);
    String headerTenant = requestContext.getHeaderString(TENANT_HEADER);
    try {
      if (queryTenant.isPresent()
          && !isMissing(headerTenant)
          && !tenantRegistry
              .normalizeTenantId(queryTenant.get())
              .equals(tenantRegistry.normalizeTenantId(headerTenant))) {
        throw new IllegalArgumentException("Conflicting tenant context");
      }
      String tenant =
          queryTenant.orElseGet(
              () ->
                  tenantEnforcementEnabled || !isMissing(headerTenant)
                      ? headerTenant
                      : defaultTenant);
      tenantRegistry.resolve(tenant);
      tenantContext.setTenantId(tenantRegistry.normalizeTenantId(tenant));
    } catch (RuntimeException exception) {
      String reason =
          queryTenant.isEmpty() && isMissing(headerTenant) ? "missing_tenant" : "invalid_tenant";
      auditRejection(reason, requestContext);
      requestContext.abortWith(problem());
    }
  }

  private Optional<String> tenantFromQuery(ContainerRequestContext requestContext) {
    String tenantId = requestContext.getUriInfo().getQueryParameters().getFirst("tenantId");
    return isMissing(tenantId) ? Optional.empty() : Optional.of(tenantId);
  }

  private boolean isMissing(String tenantId) {
    return tenantId == null || tenantId.isBlank();
  }

  private void auditRejection(String reason, ContainerRequestContext requestContext) {
    AUDIT_LOGGER.warn(
        "event=tenant_request_rejected reason={} method={} path={}",
        reason,
        sanitizeForLog(requestContext.getMethod()),
        sanitizeForLog(requestContext.getUriInfo().getPath()));
  }

  /**
   * The path is attacker-controlled, so any character outside a strict whitelist is replaced to
   * prevent forged records from being injected into the audit log.
   */
  private String sanitizeForLog(String value) {
    if (value == null) {
      return "";
    }
    String truncated =
        value.length() > MAX_LOGGED_PATH_LENGTH
            ? value.substring(0, MAX_LOGGED_PATH_LENGTH)
            : value;
    return truncated.replaceAll("[^A-Za-z0-9/._:-]", "_");
  }

  private Response problem() {
    Response.Status status = Response.Status.BAD_REQUEST;
    return Response.status(status)
        .type("application/problem+json")
        .entity(
            Problem.builder()
                .title(status.getReasonPhrase())
                .status(status.getStatusCode())
                .detail("Invalid tenant context")
                .build())
        .build();
  }
}
