package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "onboarding-functions.blob-storage")
public interface AzureStorageConfig {

  Optional<String> connectionStringContract();

  Optional<String> accountNameContract();

  Optional<String> managedIdentityClientIdContract();

  String containerContract();

  String contractPath();

  String deletedPath();


  String aggregatesPath();

}
