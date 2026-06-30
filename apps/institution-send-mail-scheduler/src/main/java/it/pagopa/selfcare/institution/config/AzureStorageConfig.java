package it.pagopa.selfcare.institution.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "institution-send-mail.blob-storage")
public interface AzureStorageConfig {

    Optional<String> connectionStringContract();

    String containerContract();

    Optional<String> contractStorageAccountName();

    Optional<String> contractManagedIdentityClientId();

    Optional<String> connectionStringProduct();

    String containerProduct();

    String productFilepath();

    Optional<String> productStorageAccountName();

    Optional<String> productManagedIdentityClientId();

}
