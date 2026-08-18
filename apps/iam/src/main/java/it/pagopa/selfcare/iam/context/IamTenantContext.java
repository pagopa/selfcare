package it.pagopa.selfcare.iam.context;

import it.pagopa.selfcare.iam.conf.TenantRegistry.Tenant;
import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped holder for the tenant resolved by {@code TenantResolutionFilter}. Populated only
 * for requests where tenant enforcement is enabled and required.
 */
@RequestScoped
public class IamTenantContext {

  private Tenant tenant;

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }

  public Tenant getTenant() {
    if (tenant == null) {
      throw new IllegalStateException("Tenant context has not been initialized");
    }
    return tenant;
  }

  public String getTenantId() {
    return getTenant().id();
  }
}
