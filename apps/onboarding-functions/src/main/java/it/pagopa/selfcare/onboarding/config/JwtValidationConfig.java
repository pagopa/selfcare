package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Configuration used to authenticate requests received by HTTP-triggered functions. */
@ConfigMapping(prefix = "onboarding-functions.jwt.validation")
public interface JwtValidationConfig {

  /** Accepted values for the JWT {@code iss} claim. */
  @WithDefault("SPID,PAGOPA")
  Set<String> allowedIssuers();

  /**
   * Tenant attributed to tokens without the {@code tenant_id} claim, emitted by legacy issuers such
   * as the PNPG SPID hub. It must still match the {@code X-Tenant-Id} header.
   */
  @WithDefault("PNPG")
  String defaultTenant();

  /** When {@code true}, tokens without the {@code tenant_id} claim are rejected. */
  @WithDefault("false")
  boolean tenantClaimRequired();

  /** RSA verification keys, indexed by canonical tenant id. */
  Map<String, TenantKey> tenants();

  interface TenantKey {

    /** RSA public key in PEM or bare Base64 X.509 format. */
    Optional<String> publicKey();
  }
}
