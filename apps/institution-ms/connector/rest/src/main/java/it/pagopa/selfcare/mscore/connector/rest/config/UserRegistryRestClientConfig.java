package it.pagopa.selfcare.mscore.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.mscore.connector.rest.interceptor.UserRegistryAuthInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, UserRegistryAuthInterceptor.class})
public class UserRegistryRestClientConfig {
}
