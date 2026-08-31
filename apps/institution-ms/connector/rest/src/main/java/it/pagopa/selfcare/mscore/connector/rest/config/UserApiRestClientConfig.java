package it.pagopa.selfcare.mscore.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor;
import it.pagopa.selfcare.mscore.connector.rest.interceptor.TenantHeaderInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, AuthorizationHeaderInterceptor.class, TenantHeaderInterceptor.class})
public class UserApiRestClientConfig {
}
