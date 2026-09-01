package it.pagopa.selfcare.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.jwt.JsonWebToken;

final class JwtTenantValidator {

  static final String TENANT_HEADER = "X-Tenant-Id";
  static final String TENANT_ATTRIBUTE = "jwt.tenant";
  private static final String CLAIM_TENANT_ID = "tenant_id";
  private static final String DEFAULT_TENANT_ID = environmentValue("DEFAULT_TENANT", "PNPG");
  private static final Set<String> SUPPORTED_TENANTS =
      parseSupportedTenants(environmentValue("SUPPORTED_TENANTS", "AR,PNPG"));

  static {
    if (!SUPPORTED_TENANTS.contains(DEFAULT_TENANT_ID)) {
      throw new IllegalArgumentException(
          "DEFAULT_TENANT must be included in SUPPORTED_TENANTS");
    }
  }

  private JwtTenantValidator() {}

  private static String environmentValue(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  private static Set<String> parseSupportedTenants(String value) {
    Set<String> tenants =
        Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(tenant -> !tenant.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    if (tenants.isEmpty()) {
      throw new IllegalArgumentException("SUPPORTED_TENANTS must contain at least one tenant");
    }
    return tenants;
  }

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
