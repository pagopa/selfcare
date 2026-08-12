package it.pagopa.selfcare.external_api.security.tenant;

/**
 * Constants for tenant resolution shared across microservices (Step_0 SELC-1..SELC-6).
 * Mirrors {@code it.pagopa.selfcare.security.tenant.TenantConstants} from the Quarkus
 * {@code selfcare-sdk-security} library; duplicated here (rather than depended on) because this
 * is a Spring Boot service and that library ships Quarkus-only runtime dependencies.
 */
public final class TenantConstants {

  /** HTTP header carrying the tenant identifier on every request (Step_0 SELC-1). */
  public static final String TENANT_HEADER = "X-Tenant-Id";

  /** JWT claim carrying the tenant identifier (Step_0 SELC-2). */
  public static final String TENANT_CLAIM = "tenant_id";

  /** JWT issuer used by the hub-spid-login flow. */
  public static final String HUB_SPID_LOGIN_ISSUER = "SPID";

  /** JWT issuer used by the OneIdentity/`auth` flow. */
  public static final String ONE_IDENTITY_ISSUER = "PAGOPA";

  /**
   * Tenant defaulted when a hub-spid-login-issued token is missing the tenant claim
   * (Step_0 SELC-3.1); the {@code X-Tenant-Id} header must still be present and equal to this
   * value (Step_0 SELC-3.4).
   */
  public static final TenantId HUB_SPID_LOGIN_DEFAULT_TENANT = TenantId.PNPG;

  /** Request attribute exposing the tenant validated by {@link TenantValidationFilter}. */
  public static final String TENANT_REQUEST_ATTRIBUTE = "validatedTenantId";

  private TenantConstants() {
  }
}
