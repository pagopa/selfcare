package it.pagopa.selfcare.security.tenant;

/**
 * Raised when the data layer asks for a per-tenant resource the tenant registry cannot answer
 * (Step_1 SELC-8.4, SELC-9.4, SELC-10.2, SELC-11.2).
 *
 * <p>It exists so that "no mapping" can never be confused with "use the default": every lookup on
 * {@link TenantDataIsolationRegistry} either returns the tenant's own value or throws this, and no
 * caller is given a fallback to reach for.
 */
public class UnresolvedTenantMappingException extends RuntimeException {

  public UnresolvedTenantMappingException(String message) {
    super(message);
  }
}
