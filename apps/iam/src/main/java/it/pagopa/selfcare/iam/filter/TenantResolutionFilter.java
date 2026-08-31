package it.pagopa.selfcare.iam.filter;

import it.pagopa.selfcare.iam.conf.TenantRegistry;
import it.pagopa.selfcare.iam.context.IamTenantContext;
import it.pagopa.selfcare.iam.controller.response.Problem;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates and sets the tenant context from the {@code X-Tenant-Id} header set by APIM, mirroring
 * the equivalent filter introduced in {@code apps/auth} (SELC-9155).
 *
 * <p>Enforcement is gated by {@code tenant.enforcement.enabled} (default {@code false}) so this
 * filter stays inert until the APIM hostname-based tenant resolution is also enabled for iam at
 * the infrastructure level; until then APIM strips the {@code X-Tenant-Id} header for this
 * service, so enforcing it unconditionally would reject all traffic.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TenantResolutionFilter implements ContainerRequestFilter {

  public static final String TENANT_HEADER = "X-Tenant-Id";
  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_TENANT");
  private static final int MAX_LOGGED_PATH_LENGTH = 200;

  @Inject TenantRegistry tenantRegistry;
  @Inject IamTenantContext tenantContext;

  @ConfigProperty(name = "tenant.enforcement.enabled", defaultValue = "false")
  boolean tenantEnforcementEnabled;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!tenantEnforcementEnabled || !requiresTenant(requestContext.getUriInfo().getPath())) {
      return;
    }

    try {
      tenantContext.setTenant(
          tenantRegistry.resolveTenant(requestContext.getHeaderString(TENANT_HEADER)));
    } catch (InvalidRequestException exception) {
      String reason =
          isMissing(requestContext.getHeaderString(TENANT_HEADER))
              ? "missing_tenant"
              : "unknown_tenant";
      auditRejection(reason, requestContext);
      requestContext.abortWith(problem(Response.Status.BAD_REQUEST, "Invalid tenant context"));
    }
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

  private boolean requiresTenant(String path) {
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    return normalizedPath.equals("iam") || normalizedPath.startsWith("iam/");
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
