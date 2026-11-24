package it.pagopa.selfcare.tenant;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the tenant of an already verified JWT from its {@code tenant_id} claim.
 *
 * <p>Framework-independent (no JAX-RS, MicroProfile JWT or JJWT types): callers pass the raw claim
 * value and map {@link InvalidTenantClaimException} to their own error. Tenant ids are compared
 * after {@code trim()} and {@code toUpperCase(Locale.ROOT)}, like {@link TenantRegistry}.
 *
 * <p>A token without the claim is attributed to the default tenant, because legacy issuers (e.g.
 * the PNPG SPID hub) do not emit it. Without a default tenant, or when the claim is required, such
 * tokens are rejected. Matching the resolved tenant with the request tenant (e.g. the {@code
 * X-Tenant-Id} header) is left to the caller.
 */
public final class JwtTenantClaimResolver {

  public static final String TENANT_CLAIM = "tenant_id";

  /** Where the resolved tenant comes from. */
  public enum Source {
    CLAIM,
    DEFAULT
  }

  public record Resolution(String tenantId, Source source) {}

  private final String defaultTenantId;
  private final Set<String> supportedTenants;
  private final boolean tenantClaimRequired;

  private JwtTenantClaimResolver(
      String defaultTenantId, Set<String> supportedTenants, boolean tenantClaimRequired) {
    this.defaultTenantId = defaultTenantId;
    this.supportedTenants = supportedTenants;
    this.tenantClaimRequired = tenantClaimRequired;
  }

  /**
   * @param defaultTenantId tenant of tokens without the claim; {@code null} or blank for none
   * @param supportedTenants tenants accepted in the claim; must not be empty
   * @param tenantClaimRequired when {@code true}, tokens without the claim are rejected
   * @throws IllegalArgumentException if no tenant is supported or the default tenant is not
   *     supported
   */
  public static JwtTenantClaimResolver of(
      String defaultTenantId, Collection<String> supportedTenants, boolean tenantClaimRequired) {
    Set<String> tenants =
        supportedTenants.stream()
            .filter(Objects::nonNull)
            .filter(tenant -> !tenant.isBlank())
            .map(JwtTenantClaimResolver::normalize)
            .collect(Collectors.toUnmodifiableSet());
    if (tenants.isEmpty()) {
      throw new IllegalArgumentException("Supported tenants must contain at least one tenant");
    }
    String defaultTenant =
        defaultTenantId == null || defaultTenantId.isBlank() ? null : normalize(defaultTenantId);
    if (defaultTenant != null && !tenants.contains(defaultTenant)) {
      throw new IllegalArgumentException("Default tenant must be one of the supported tenants");
    }
    return new JwtTenantClaimResolver(defaultTenant, tenants, tenantClaimRequired);
  }

  /**
   * @param rawTenantClaim value of the {@code tenant_id} claim, {@code null} when absent
   * @throws InvalidTenantClaimException if the claim is missing and no default applies, or it is not
   *     a non-blank string naming a supported tenant
   */
  public Resolution resolve(Object rawTenantClaim) {
    if (rawTenantClaim == null) {
      if (tenantClaimRequired) {
        throw new InvalidTenantClaimException("Missing tenant_id claim");
      }
      if (defaultTenantId == null) {
        throw new InvalidTenantClaimException("No default tenant configured");
      }
      return new Resolution(defaultTenantId, Source.DEFAULT);
    }
    if (!(rawTenantClaim instanceof String tenantId) || tenantId.isBlank()) {
      throw new InvalidTenantClaimException("Invalid tenant_id claim");
    }
    String normalizedTenantId = normalize(tenantId);
    if (!supportedTenants.contains(normalizedTenantId)) {
      throw new InvalidTenantClaimException("Unsupported tenant_id claim");
    }
    return new Resolution(normalizedTenantId, Source.CLAIM);
  }

  public static String normalize(String tenantId) {
    return tenantId.trim().toUpperCase(Locale.ROOT);
  }
}
