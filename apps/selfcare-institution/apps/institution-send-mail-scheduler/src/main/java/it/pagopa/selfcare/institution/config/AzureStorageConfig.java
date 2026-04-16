package it.pagopa.selfcare.institution.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "institution-send-mail.blob-storage")
public interface AzureStorageConfig {

    String connectionStringContract();
    String containerContract();

    String connectionStringProduct();

    String containerProduct();

    String productFilepath();
}
