package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class AzureSearchClientCredentials {

  @Bean
  public AzureSearchApiKeyRequestInterceptor azureSearchApiKeyInterceptor(
      @Value("${AZURE_SEARCH_API_KEY:x}") String apiKey) {
    return new AzureSearchApiKeyRequestInterceptor(apiKey);
  }
}
