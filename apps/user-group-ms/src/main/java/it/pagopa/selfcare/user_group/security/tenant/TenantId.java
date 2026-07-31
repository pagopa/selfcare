package it.pagopa.selfcare.user_group.security.tenant;

import java.util.Arrays;
import java.util.Optional;

/**
 * Canonical tenant identifiers for the Selfcare multitenant backend (Step_0 SELC-6).
 * {@code AR} identifies {@code selfcare.pagopa.it}, {@code PNPG} identifies
 * {@code imprese.notifichedigitali.it}.
 */
public enum TenantId {
  AR,
  PNPG;

  /**
   * Resolves a raw string (HTTP header or JWT claim value) to a known {@link TenantId}.
   * Comparison is case-insensitive; unknown or blank values resolve to an empty {@link Optional}
   * so callers can reject the request instead of defaulting to a tenant (Step_0 SELC-1.3).
   */
  public static Optional<TenantId> fromValue(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    String trimmed = value.trim();
    return Arrays.stream(values())
        .filter(tenant -> tenant.name().equalsIgnoreCase(trimmed))
        .findFirst();
  }
}
