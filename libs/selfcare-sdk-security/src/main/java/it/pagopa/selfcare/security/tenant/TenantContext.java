package it.pagopa.selfcare.security.tenant;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the tenant identity already validated for the current request (Step_0 SELC-1/SELC-2),
 * for downstream consumption by business and data-access code (prepares for Step_1 tenant-scoped
 * Cosmos DB / Storage / vault / email isolation).
 *
 * <p>Populated exclusively by {@link TenantValidationFilter} after the header/claim reconciliation
 * succeeds; other code must treat this as read-only.
 */
@RequestScoped
public class TenantContext {

  private TenantId tenant;

  public TenantId getTenant() {
    return tenant;
  }

  void setTenant(TenantId tenant) {
    this.tenant = tenant;
  }
}
