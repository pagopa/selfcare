package it.pagopa.selfcare.security.tenant;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enforces tenant consistency on every authenticated request (Step_0 SELC-1, SELC-2, SELC-3):
 *
 * <ul>
 *   <li>rejects requests with a missing, duplicated, or unknown {@code X-Tenant-Id} header
 *       (SELC-1.3) — no silent default is ever applied to the header;</li>
 *   <li>reconciles the header with the JWT {@code tenant_id} claim, rejecting on mismatch
 *       (SELC-2.3);</li>
 *   <li>defaults the claim to {@code PNPG} only for hub-spid-login-issued tokens missing it
 *       (SELC-3.1), while still requiring the header to be present and equal to {@code PNPG}
 *       (SELC-3.4);</li>
 *   <li>exposes the validated tenant via {@link TenantContext} for downstream code.</li>
 * </ul>
 *
 * <p>Requests without an authenticated JWT principal (e.g. public/health endpoints) are not
 * enforced here, since there is no session tenant claim to reconcile against; this scoping
 * follows SELC-2.2 ("for the lifetime of the authenticated session").
 */
@ApplicationScoped
@Provider
@Priority(Priorities.AUTHENTICATION + 100)
public class TenantValidationFilter implements ContainerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantValidationFilter.class);

  @Inject
  JsonWebToken jwt;

  @Inject
  TenantContext tenantContext;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String issuer = jwt.getIssuer();
    if (issuer == null || issuer.isEmpty()) {
      // No authenticated session for this request; nothing to enforce here.
      return;
    }

    Optional<TenantId> headerTenant = resolveHeaderTenant(requestContext);
    if (headerTenant.isEmpty()) {
      reject(requestContext, "Missing, duplicated, or unknown X-Tenant-Id header");
      return;
    }

    Optional<TenantId> claimTenant = resolveClaimTenant(issuer);
    if (claimTenant.isEmpty()) {
      reject(requestContext, "Missing or unknown tenant_id claim in JWT");
      return;
    }

    if (headerTenant.get() != claimTenant.get()) {
      LOGGER.warn(
          "Tenant mismatch rejected: header tenant={}, claim tenant={}, issuer={}",
          headerTenant.get(), claimTenant.get(), issuer);
      reject(requestContext, "X-Tenant-Id header does not match the JWT tenant claim");
      return;
    }

    tenantContext.setTenant(headerTenant.get());
  }

  private Optional<TenantId> resolveHeaderTenant(ContainerRequestContext requestContext) {
    String headerValue = requestContext.getHeaderString(TenantConstants.TENANT_HEADER);
    if (headerValue != null && headerValue.contains(",")) {
      // Multiple X-Tenant-Id header values on the same request: treat as invalid/spoofed input,
      // never pick one silently.
      LOGGER.warn("Rejected request carrying multiple X-Tenant-Id header values");
      return Optional.empty();
    }
    return TenantId.fromValue(headerValue);
  }

  private Optional<TenantId> resolveClaimTenant(String issuer) {
    String claimValue = jwt.getClaim(TenantConstants.TENANT_CLAIM);
    if (claimValue == null || claimValue.isBlank()) {
      if (TenantConstants.HUB_SPID_LOGIN_ISSUER.equals(issuer)) {
        LOGGER.info(
            "hub-spid-login token missing tenant_id claim; defaulting to {} (Step_0 SELC-3.1)",
            TenantConstants.HUB_SPID_LOGIN_DEFAULT_TENANT);
        return Optional.of(TenantConstants.HUB_SPID_LOGIN_DEFAULT_TENANT);
      }
      return Optional.empty();
    }
    return TenantId.fromValue(claimValue);
  }

  private void reject(ContainerRequestContext requestContext, String detail) {
    LOGGER.warn("Tenant validation rejected request: {}", detail);
    requestContext.abortWith(
        Response.status(Response.Status.BAD_REQUEST)
            .type("application/problem+json")
            .entity(new TenantProblem(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid tenant", detail))
            .build());
  }
}
