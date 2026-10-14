package it.pagopa.selfcare.security;

import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Validates the {@code tenant_id} claim of SPID tokens and the {@code X-Tenant-Id} header against
 * the tenants defined in the {@link TenantRegistry} ({@code tenant.registry.json}). Values are
 * compared exactly. A token without the claim belongs to {@code DEFAULT_TENANT} (default {@code
 * PNPG}).
 */
@Singleton
class JwtTenantValidator {

  static final String TENANT_HEADER = "X-Tenant-Id";
  static final String TENANT_ATTRIBUTE = "jwt.tenant";
  private static final String CLAIM_TENANT_ID = "tenant_id";

  private final TenantRegistry tenantRegistry;
  private final String defaultTenantId;

  @Inject
  JwtTenantValidator(
      TenantRegistry tenantRegistry,
      @ConfigProperty(name = "DEFAULT_TENANT", defaultValue = "PNPG") String defaultTenantId) {
    this.tenantRegistry = tenantRegistry;
    this.defaultTenantId = defaultTenantId;
  }

  String resolveTokenTenant(JsonWebToken jwt) {
    Object rawTenantId = jwt.getClaim(CLAIM_TENANT_ID);
    if (rawTenantId == null) {
      return defaultTenantId;
    }
    if (rawTenantId instanceof String tenantId && isKnownTenant(tenantId)) {
      return tenantId;
    }
    throw new TenantValidationException();
  }

  void validateHeader(String tenantId, String headerTenantId) {
    if (!isKnownTenant(headerTenantId) || !tenantId.equals(headerTenantId)) {
      throw new TenantValidationException();
    }
  }

  /** Claim and header must match a registry tenant exactly: non-canonical values are rejected. */
  private boolean isKnownTenant(String tenantId) {
    return tenantId != null
        && !tenantId.isBlank()
        && tenantRegistry.definitions().containsKey(tenantId);
  }
}
