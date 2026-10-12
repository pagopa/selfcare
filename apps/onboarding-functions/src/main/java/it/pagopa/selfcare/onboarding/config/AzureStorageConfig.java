package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.blob-storage")
public interface AzureStorageConfig {

  String contractPath();

  String deletedPath();

  String aggregatesPath();

}
