package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(RestClientBaseConfig.class)
@EnableFeignClients(clients = AzureSearchRestClient.class)
@PropertySource("classpath:config/azure-ai-search-rest-client.properties")
public class AzureSearchRestClientConfig {

  @Value("${AZURE_SEARCH_API_KEY:x}")
  private String apiKey;

  @Bean
  public AzureSearchApiKeyRequestInterceptor azureSearchApiKeyInterceptor() {
    return new AzureSearchApiKeyRequestInterceptor(apiKey);
  }

}
