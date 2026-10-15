package it.pagopa.selfcare.security.tenant;

import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Resolves the optional {@code tenantId} query parameter of an endpoint. When omitted, the value
 * comes from the {@code X-Tenant-Id} header (already stored in {@link TenantContext}) or, if that
 * is absent, from the JWT {@code tenant_id} claim. A tenant in conflict with the header or the
 * claim is rejected with 400. The security attribute {@code jwt.tenant} is not a default source.
 *
 * <ul>
 *   <li>{@link #resolve(String)} returns the tenant, for services that receive it as a parameter;
 *   <li>{@link #bindToContext(String)} also stores it in {@link TenantContext}, for services that
 *       read the tenant from the context.
 * </ul>
 */
@ApplicationScoped
public class EffectiveTenant {

  public static final String QUERY_DESCRIPTION =
      "Optional. Defaults to the X-Tenant-Id header, or to the JWT tenant_id claim when the header is absent.";

  public static final String JWT_TENANT_CLAIM = "tenant_id";

  private final TenantContext tenantContext;
  private final TenantRegistry tenantRegistry;
  private final SecurityIdentity securityIdentity;

  @Inject
  public EffectiveTenant(
      TenantContext tenantContext,
      TenantRegistry tenantRegistry,
      SecurityIdentity securityIdentity) {
    this.tenantContext = tenantContext;
    this.tenantRegistry = tenantRegistry;
    this.securityIdentity = securityIdentity;
  }

  public String resolve(String requestedTenantId) {
    String trustedTenant = trustedTenant();
    if (requestedTenantId == null || requestedTenantId.isBlank()) {
      if (trustedTenant == null) {
        throw new BadRequestException("Missing tenantId");
      }
      return trustedTenant;
    }
    if (trustedTenant != null && !sameTenant(requestedTenantId, trustedTenant)) {
      throw new BadRequestException("Conflicting tenant context");
    }
    return requestedTenantId;
  }

  public void bindToContext(String requestedTenantId) {
    String tenant = tenantRegistry.normalizeTenantId(resolve(requestedTenantId));
    try {
      tenantRegistry.resolve(tenant);
    } catch (RuntimeException exception) {
      throw new BadRequestException("Unknown tenant");
    }
    tenantContext.setTenantId(tenant);
  }

  private String trustedTenant() {
    String contextTenant = tenantContext.isInitialized() ? tenantContext.getTenantId() : null;
    String jwtTenant = jwtTenant();
    if (contextTenant != null && jwtTenant != null && !sameTenant(contextTenant, jwtTenant)) {
      throw new BadRequestException("Conflicting tenant context");
    }
    return contextTenant != null ? contextTenant : jwtTenant;
  }

  private String jwtTenant() {
    if (!(securityIdentity.getPrincipal() instanceof JsonWebToken jwt)) {
      return null;
    }
    Object claim = jwt.getClaim(JWT_TENANT_CLAIM);
    if (claim instanceof String value && !value.isBlank()) {
      return value;
    }
    return null;
  }

  private boolean sameTenant(String left, String right) {
    return tenantRegistry.normalizeTenantId(left).equals(tenantRegistry.normalizeTenantId(right));
  }
}
