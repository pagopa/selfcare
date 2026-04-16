package it.pagopa.selfcare.party.registry_proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "it.pagopa.selfcare.party.*")
@EnableScheduling
public class SelfCarePartyRegistryProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCarePartyRegistryProxyApplication.class, args);
    }

}
