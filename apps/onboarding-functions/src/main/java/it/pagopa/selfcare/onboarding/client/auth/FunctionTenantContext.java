package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Carries the tenant of the onboarding an activity is currently processing, so outgoing calls to
 * tenant-enforcing services can be made on that tenant's behalf.
 *
 * <p>A durable-function activity has no incoming HTTP request, so there is no request scope to read
 * the tenant from and no {@code TenantContext} to reuse - the tenant can only come from the
 * orchestration payload. This holder is deliberately thread-bound rather than {@code
 * @RequestScoped}: activities run synchronously on the invocation thread and this application never
 * activates a CDI request context.
 *
 * <p>The value is <b>always overwritten</b> when an onboarding is deserialised, including with
 * {@code null} when that onboarding carries no tenant. Leaving a previous value in place would let
 * one onboarding's tenant leak onto calls made for another, which is precisely the cross-tenant
 * mistake tenant enforcement exists to prevent.
 */
@ApplicationScoped
public class FunctionTenantContext {

  /**
   * Kept in sync by hand with {@code TenantConstants} in selfcare-sdk-security. That library is not
   * a dependency here: it would drag quarkus-smallrye-jwt into an application that does not
   * otherwise use it, changing its JWT configuration for the sake of two string constants.
   */
  public static final String TENANT_CLAIM = "tenant_id";

  public static final String TENANT_HEADER = "X-Tenant-Id";

  private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

  /** Overwrites the tenant for the current activity; {@code null} clears it. */
  public static void set(String tenantId) {
    if (tenantId == null) {
      CURRENT_TENANT.remove();
    } else {
      CURRENT_TENANT.set(tenantId);
    }
  }

  public String currentTenantId() {
    return CURRENT_TENANT.get();
  }

  public static void clear() {
    CURRENT_TENANT.remove();
  }
}
