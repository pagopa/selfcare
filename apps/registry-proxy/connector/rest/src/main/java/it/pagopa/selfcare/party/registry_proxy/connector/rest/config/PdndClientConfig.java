package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PdndRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration
@Import(RestClientBaseConfig.class)
@PropertySource("classpath:config/pdnd-rest-client.properties")
@EnableFeignClients(clients = PdndRestClient.class)
public class PdndClientConfig { }