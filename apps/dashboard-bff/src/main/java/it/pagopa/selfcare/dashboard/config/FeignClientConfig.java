package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.client.*;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/feign-client.properties")
@PropertySource("classpath:config/iam-rest-client.properties")
@PropertySource("classpath:config/ms-core-rest-client.properties")
@PropertySource("classpath:config/onboarding-rest-client.properties")
@PropertySource("classpath:config/pago-pa-backoffice-rest-client.properties")
@PropertySource("classpath:config/user-rest-client.properties")
@PropertySource("classpath:config/user-group-rest-client.properties")
@PropertySource("classpath:config/user-registry-rest-client.properties")
@PropertySource("classpath:config/document-rest-client.properties")
@EnableFeignClients(clients = {
        UserApiRestClient.class,
        IamExternalRestClient.class,
        IamRestClient.class,
        CoreDelegationApiRestClient.class,
        CoreInstitutionApiRestClient.class,
        CoreOnboardingApiRestClient.class,
        CoreManagementApiRestClient.class,
        OnboardingRestClient.class,
        MsBackOfficeStationApiClient.class,
        MsBackOfficeChannelApiClient.class,
        UserGroupRestClient.class,
        UserInstitutionApiRestClient.class,
        UserPermissionRestClient.class,
        UserRegistryRestClient.class,
        DocumentRestClient.class,
        DocumentContentRestClient.class
})
public class FeignClientConfig {
}
