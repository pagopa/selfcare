package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.InstitutionRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({RestClientBaseConfig.class})
@EnableFeignClients(clients = InstitutionRestClient.class)
@PropertySource("classpath:config/selc-institution-rest-client.properties")
public class InstitutionRestClientConfig {

    @Value("${JWT-BEARER-TOKEN-FUNCTIONS:test}")
    private String bearerToken;

    @Bean
    public JwtTokenRequestInterceptor geoJwtTokenRequestInterceptor() {
        return new JwtTokenRequestInterceptor(bearerToken);
    }

}
