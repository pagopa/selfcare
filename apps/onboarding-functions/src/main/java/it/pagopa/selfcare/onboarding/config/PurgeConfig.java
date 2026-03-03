package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.purge")
public interface PurgeConfig {

    Long completedFrom();
    Long completedTo();
    Long allFrom();
    Long allTo();

}
