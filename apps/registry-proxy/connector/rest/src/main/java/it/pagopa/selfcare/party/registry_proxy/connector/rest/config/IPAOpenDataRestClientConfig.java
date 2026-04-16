package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.IPAOpenDataRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration
@Import(RestClientBaseConfig.class)
@EnableFeignClients(clients = IPAOpenDataRestClient.class)
@PropertySource("classpath:config/ipa-open-data-rest-client.properties")
class IPAOpenDataRestClientConfig {

    public IPAOpenDataRestClientConfig() {
        log.trace("Initializing {}", IPAOpenDataRestClientConfig.class.getSimpleName());
    }

}
