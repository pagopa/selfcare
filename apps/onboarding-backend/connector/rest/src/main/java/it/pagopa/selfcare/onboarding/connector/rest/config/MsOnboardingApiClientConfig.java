package it.pagopa.selfcare.onboarding.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.onboarding.connector.rest.client.*;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(RestClientBaseConfig.class)
@EnableFeignClients(clients = {MsOnboardingApiClient.class, MsOnboardingTokenApiClient.class, MsOnboardingSupportApiClient.class, MsOnboardingAggregatesApiClient.class, MsOnboardingInstitutionApiClient.class, MsOnboardingInternalApiClient.class, MsOnboardingBillingApiClient.class})
@PropertySource("classpath:config/ms-onboarding-rest-client.properties")
public class MsOnboardingApiClientConfig {
}
