package it.pagopa.selfcare.product.filter;

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
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class TenantResolutionFilter implements ContainerRequestFilter {

  public static final String TENANT_HEADER = "X-Tenant-Id";

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
    if (path.startsWith("q/") || path.equals("q")) {
      return;
    }

    String headerTenant = requestContext.getHeaderString(TENANT_HEADER);
    try {
      String tenant =
          tenantEnforcementEnabled
              ? headerTenant
              : (headerTenant == null || headerTenant.isBlank() ? defaultTenant : headerTenant);
      tenantRegistry.resolve(tenant);
      tenantContext.setTenantId(tenantRegistry.normalizeTenantId(tenant));
    } catch (RuntimeException exception) {
      requestContext.abortWith(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid tenant context")
              .type("application/problem+json")
              .build());
    }
  }
}
