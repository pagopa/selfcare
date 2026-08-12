package it.pagopa.selfcare.security.tenant;

/**
 * Constants for tenant resolution shared across microservices (Step_0 SELC-1..SELC-6).
 * Kept in one place so the header name, claim name, and issuer identifiers cannot drift between
 * services.
 */
public final class TenantConstants {

  /** HTTP header carrying the tenant identifier on every request (Step_0 SELC-1). */
  public static final String TENANT_HEADER = "X-Tenant-Id";

  /**
   * JWT claim carrying the tenant identifier (Step_0 SELC-2). Name fixed as part of this
   * implementation; REQUIREMENTS.md left the exact claim name as an open question.
   */
  public static final String TENANT_CLAIM = "tenant_id";

  /**
   * JWT issuer used by the hub-spid-login flow. Must match the issuer already recognized by
   * {@link it.pagopa.selfcare.security.JWTCallerPrincipalFactory}.
   */
  public static final String HUB_SPID_LOGIN_ISSUER = "SPID";

  /**
   * JWT issuer used by the OneIdentity/`auth` flow. Must match the issuer already recognized by
   * {@link it.pagopa.selfcare.security.JWTCallerPrincipalFactory}.
   */
  public static final String ONE_IDENTITY_ISSUER = "PAGOPA";

  /**
   * Tenant defaulted when a hub-spid-login-issued token is missing the tenant claim
   * (Step_0 SELC-3.1); the {@code X-Tenant-Id} header must still be present and equal to this
   * value (Step_0 SELC-3.4).
   */
  public static final TenantId HUB_SPID_LOGIN_DEFAULT_TENANT = TenantId.PNPG;

  private TenantConstants() {
  }
}
