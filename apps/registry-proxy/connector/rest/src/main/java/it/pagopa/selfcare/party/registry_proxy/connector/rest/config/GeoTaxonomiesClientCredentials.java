package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class GeoTaxonomiesClientCredentials {

  @Bean
  public ApiKeyRequestInterceptor geoTaxonomiesApiKeyInterceptor(
      @Value("${GEOTAXONOMY_API_KEY:test}") String apiKey) {
    return new ApiKeyRequestInterceptor(apiKey);
  }
}
