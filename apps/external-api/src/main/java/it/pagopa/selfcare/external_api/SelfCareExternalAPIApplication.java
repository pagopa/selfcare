package it.pagopa.selfcare.external_api;

import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@Import(TenantConfiguration.class)
@PropertySource("classpath:config/tenant-config.properties")
public class SelfCareExternalAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCareExternalAPIApplication.class, args);
    }

}
