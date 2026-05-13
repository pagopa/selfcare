package it.pagopa.selfcare.external_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration
@PropertySource("classpath:config/azure-storage-config.properties")
@Profile("AzureStorage")
class AzureStorageConfig {

    public AzureStorageConfig(){
        log.trace("Initializing {}...", AzureStorageConfig.class.getSimpleName());
    }
}
