package it.pagopa.selfcare.commons.web.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AuthenticationV1Api;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import org.springframework.context.annotation.Bean;

class K8sAuthenticationConfig {

    @Bean
    public AuthenticationV1Api getAuthenticationV1Api() throws IOException {
        final ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return new AuthenticationV1Api();
    }

}
