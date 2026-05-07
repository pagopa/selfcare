package it.pagopa.selfcare.dashboard.config.restclient;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.dashboard.interceptor.BackOfficeAuthorizationInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, BackOfficeAuthorizationInterceptor.class})
public class PagoPABackOfficeRestClientConfig {
}
