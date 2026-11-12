package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;
import java.util.Set;

/** Configuration used to authenticate requests received by HTTP-triggered functions. */
@ConfigMapping(prefix = "onboarding-functions.jwt.validation")
public interface JwtValidationConfig {

  /** Accepted values for the JWT {@code iss} claim. */
  @WithDefault("SPID,PAGOPA")
  Set<String> allowedIssuers();

  /**
   * Tenant attributed to tokens without the {@code tenant_id} claim ({@code JWT_DEFAULT_TENANT}); it
   * must still match the {@code X-Tenant-Id} header. There is no default in code: each stack sets
   * its own tenant (legacy PNPG SPID hub tokens and AR backstage tokens do not emit the claim) and,
   * when it is missing, tokens without the claim are rejected. Once every issuer emits the claim,
   * enable {@link #tenantClaimRequired()} and remove this fallback.
   */
  Optional<String> defaultTenant();

  /** When {@code true}, tokens without the {@code tenant_id} claim are rejected. */
  @WithDefault("false")
  boolean tenantClaimRequired();
}
