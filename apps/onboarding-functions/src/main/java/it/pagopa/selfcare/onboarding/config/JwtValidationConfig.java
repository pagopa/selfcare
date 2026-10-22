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

  /** RSA verification keys, indexed by canonical tenant id. */
  Map<String, TenantKey> tenants();

  interface TenantKey {

    /** RSA public key in PEM or bare Base64 X.509 format. */
    Optional<String> publicKey();
  }
}
