package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.retry")
public interface RetryPolicyConfig {

    Integer maxAttempts();
    Long firstRetryInterval();
    Double backoffCoefficient();

}
