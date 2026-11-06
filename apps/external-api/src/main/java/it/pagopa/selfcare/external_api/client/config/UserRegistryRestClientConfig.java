package it.pagopa.selfcare.external_api.client.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.external_api.client.interceptor.UserRegistryAuthInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, UserRegistryAuthInterceptor.class})
public class UserRegistryRestClientConfig {
}
