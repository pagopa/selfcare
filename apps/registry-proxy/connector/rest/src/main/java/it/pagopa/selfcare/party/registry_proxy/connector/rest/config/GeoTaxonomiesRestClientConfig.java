package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.GeoTaxonomiesRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({RestClientBaseConfig.class})
@EnableFeignClients(clients = GeoTaxonomiesRestClient.class)
@PropertySource("classpath:config/geo-taxonomies-rest-client.properties")
public class GeoTaxonomiesRestClientConfig {

    @Value("${GEOTAXONOMY_API_KEY:test}")
    private String apiKey;

    @Bean
    public ApiKeyRequestInterceptor geoTaxonomiesApiKeyInterceptor() {
        return new ApiKeyRequestInterceptor(apiKey);
    }

}
