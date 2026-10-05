package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "onboarding-functions.blob-storage")
public interface AzureStorageConfig {

  Optional<String> connectionStringProduct();

  Optional<String> accountNameProduct();

  Optional<String> managedIdentityClientIdProduct();

  String containerProduct();

  String contractPath();

  String deletedPath();

  String productFilepath();

  String aggregatesPath();

}
