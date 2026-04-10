package it.pagopa.selfcare.onboarding.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsDocumentContentApiClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(RestClientBaseConfig.class)
@EnableFeignClients(clients = {MsDocumentContentApiClient.class, MsDocumentApiClient.class})
@PropertySource("classpath:config/ms-document-rest-client.properties")
public class MsDocumentApiClientConfig {
}
