package it.pagopa.selfcare.auth.context;

import it.pagopa.selfcare.auth.conf.TenantRegistry.Tenant;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthTenantContext {

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
