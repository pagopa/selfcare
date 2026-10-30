package it.pagopa.selfcare.external_api.config;

import it.pagopa.selfcare.commons.tenant.storage.TenantStorageConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Import(TenantStorageConfiguration.class)
@Profile("AzureStorage")
class AzureStorageConfig {

    public AzureStorageConfig(){
        log.trace("Initializing {}...", AzureStorageConfig.class.getSimpleName());
    }
}
