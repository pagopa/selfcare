package it.pagopa.selfcare.security;

import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;

final class JwtTenantValidator {

  static final String TENANT_HEADER = "X-Tenant-Id";
  static final String TENANT_ATTRIBUTE = "jwt.tenant";
  private static final String CLAIM_TENANT_ID = "tenant_id";
  private static final String DEFAULT_TENANT_ID = "PNPG";
  private static final Set<String> SUPPORTED_TENANTS = Set.of("AR", DEFAULT_TENANT_ID);

  private JwtTenantValidator() {}

  static String resolveTokenTenant(JsonWebToken jwt) {
    Object rawTenantId = jwt.getClaim(CLAIM_TENANT_ID);
    if (rawTenantId == null) {
      return DEFAULT_TENANT_ID;
    }
    if (rawTenantId instanceof String tenantId
        && !tenantId.isBlank()
        && SUPPORTED_TENANTS.contains(tenantId)) {
      return tenantId;
    }
    throw new TenantValidationException();
  }

  static void validateHeader(String tenantId, String headerTenantId) {
    if (headerTenantId == null
        || headerTenantId.isBlank()
        || !SUPPORTED_TENANTS.contains(headerTenantId)
        || !tenantId.equals(headerTenantId)) {
      throw new TenantValidationException();
    }
  }
}
