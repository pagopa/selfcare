package it.pagopa.selfcare.external_api.client.config;

import it.pagopa.selfcare.external_api.client.*;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/feign-client.properties")
@PropertySource("classpath:config/ms-core-rest-client.properties")
@PropertySource("classpath:config/ms-onboarding-rest-client.properties")
@PropertySource("classpath:config/ms-party-registry-proxy-rest-client.properties")
@PropertySource("classpath:config/ms-user-rest-client.properties")
@PropertySource("classpath:config/user-registry-rest-client.properties")
@PropertySource("classpath:config/ms-document-rest-client.properties")
@EnableFeignClients(clients = {
        MsCoreRestClient.class,
        MsCoreInstitutionApiClient.class,
        MsOnboardingControllerApi.class,
        MsPartyRegistryProxyRestClient.class,
        MsRegistryProxyNationalRegistryRestClient.class,
        MsUserApiRestClient.class,
        UserRegistryRestClient.class,
        MsDocumentContentApiClient.class,
        MsDocumentApiClient.class,
})
public class FeignClientConfig {
}
