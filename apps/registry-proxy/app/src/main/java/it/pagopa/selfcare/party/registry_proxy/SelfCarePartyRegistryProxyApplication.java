package it.pagopa.selfcare.party.registry_proxy;

import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import it.pagopa.selfcare.commons.tenant.storage.TenantStorageConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "it.pagopa.selfcare.party.*")
@EnableScheduling
@Import({TenantConfiguration.class, TenantStorageConfiguration.class})
@PropertySource("classpath:config/tenant-config.properties")
public class SelfCarePartyRegistryProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCarePartyRegistryProxyApplication.class, args);
    }

}
