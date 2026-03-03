package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "external")
public interface ExternalConfig {
    boolean byPassCheckOrganization();

    String grantType();

    String clientId();

    String clientSecret();
}