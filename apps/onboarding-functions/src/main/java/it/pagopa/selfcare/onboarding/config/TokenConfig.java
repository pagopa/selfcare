package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.jwt.token")
public interface TokenConfig {
    String signingKey();
    String kid();
    String issuer();
    String duration();
}