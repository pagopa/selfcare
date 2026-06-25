package it.pagopa.selfcare.dashboard.config.restclient;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, AuthorizationHeaderInterceptor.class})
public class IamExternalRestClientConfig {
}