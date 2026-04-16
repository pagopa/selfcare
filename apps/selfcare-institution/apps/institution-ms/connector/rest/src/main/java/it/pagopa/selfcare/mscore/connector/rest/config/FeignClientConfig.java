package it.pagopa.selfcare.mscore.connector.rest.config;


import it.pagopa.selfcare.mscore.connector.rest.client.*;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/feign-client.properties")
@PropertySource("classpath:config/event-hub-rest-client.properties")
@PropertySource("classpath:config/party-registry-proxy-rest-client.properties")
@PropertySource("classpath:config/user-rest-client.properties")
@PropertySource("classpath:config/user-registry-rest-client.properties")
@EnableFeignClients(clients = {
        UserRegistryRestClient.class,
        UserInstitutionApiRestClient.class,
        UserApiRestClient.class,
        PartyRegistryProxyRestClient.class,
        EventHubRestClient.class
})
public class FeignClientConfig {
}
