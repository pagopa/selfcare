package it.pagopa.selfcare.security;

import it.pagopa.selfcare.tenant.InvalidTenantClaimException;
import it.pagopa.selfcare.tenant.JwtTenantClaimResolver;
import java.util.Arrays;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Reconciles the tenant of a verified SPID token with the {@code X-Tenant-Id} header.
 *
 * <p>The claim is resolved by the shared {@link JwtTenantClaimResolver}: tenant ids are compared
 * after {@code trim()} and {@code toUpperCase(Locale.ROOT)}, like the tenant registry and the
 * Spring {@code SpidJwtAuthenticationStrategy}. A token without the {@code tenant_id} claim is
 * attributed to {@code DEFAULT_TENANT}, because legacy issuers (e.g. the PNPG SPID hub) do not emit
 * it; set {@code JWT_TENANT_CLAIM_REQUIRED=true} to reject such tokens once every issuer of the
 * environment emits the claim.
 */
final class JwtTenantValidator {

  static final String TENANT_HEADER = "X-Tenant-Id";
  static final String TENANT_ATTRIBUTE = "jwt.tenant";
  static final String DEFAULT_TENANT_ENV = "DEFAULT_TENANT";
  static final String SUPPORTED_TENANTS_ENV = "SUPPORTED_TENANTS";
  static final String TENANT_CLAIM_REQUIRED_ENV = "JWT_TENANT_CLAIM_REQUIRED";
  private static final JwtTenantValidator FROM_ENVIRONMENT = fromEnvironment(System::getenv);

  private final JwtTenantClaimResolver resolver;

  private JwtTenantValidator(
      String defaultTenantId, Set<String> supportedTenants, boolean tenantClaimRequired) {
    if (!supportedTenants.contains(defaultTenantId)) {
      throw new IllegalArgumentException(
          DEFAULT_TENANT_ENV + " must be included in " + SUPPORTED_TENANTS_ENV);
    }
    this.resolver =
        JwtTenantClaimResolver.of(defaultTenantId, supportedTenants, tenantClaimRequired);
  }

  static JwtTenantValidator fromEnvironment() {
    return FROM_ENVIRONMENT;
  }

  static JwtTenantValidator fromEnvironment(UnaryOperator<String> environment) {
    return new JwtTenantValidator(
        normalize(environmentValue(environment, DEFAULT_TENANT_ENV, "PNPG")),
        parseSupportedTenants(environmentValue(environment, SUPPORTED_TENANTS_ENV, "AR,PNPG")),
        parseBoolean(environmentValue(environment, TENANT_CLAIM_REQUIRED_ENV, "false")));
  }

  private static String environmentValue(
      UnaryOperator<String> environment, String name, String defaultValue) {
    String value = environment.apply(name);
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  private static Set<String> parseSupportedTenants(String value) {
    Set<String> tenants =
        Arrays.stream(value.split(","))
            .filter(tenant -> !tenant.isBlank())
            .map(JwtTenantValidator::normalize)
            .collect(Collectors.toUnmodifiableSet());
    if (tenants.isEmpty()) {
      throw new IllegalArgumentException(
          SUPPORTED_TENANTS_ENV + " must contain at least one tenant");
    }
    return tenants;
  }

  private static boolean parseBoolean(String value) {
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(TENANT_CLAIM_REQUIRED_ENV + " must be true or false");
  }

  private static String normalize(String tenantId) {
    return JwtTenantClaimResolver.normalize(tenantId);
  }

  String resolveTokenTenant(JsonWebToken jwt) {
    try {
      return resolver.resolve(jwt.getClaim(JwtTenantClaimResolver.TENANT_CLAIM)).tenantId();
    } catch (InvalidTenantClaimException e) {
      throw new TenantValidationException();
    }
  }

  void validateHeader(String tenantId, String headerTenantId) {
    if (headerTenantId == null
        || headerTenantId.isBlank()
        || !tenantId.equals(normalize(headerTenantId))) {
      throw new TenantValidationException();
    }
  }
}
