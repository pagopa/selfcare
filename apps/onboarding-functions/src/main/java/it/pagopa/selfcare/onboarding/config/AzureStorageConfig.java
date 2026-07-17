package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "onboarding-functions.blob-storage")
public interface AzureStorageConfig {

  Optional<String> connectionStringContract();

  Optional<String> connectionStringProduct();

  Optional<String> accountNameContract();

  Optional<String> accountNameProduct();

  Optional<String> managedIdentityClientIdContract();

  Optional<String> managedIdentityClientIdProduct();

  String containerContract();

  String containerProduct();

  String contractPath();

  String deletedPath();

  String productFilepath();

  String aggregatesPath();

}
