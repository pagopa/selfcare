package it.pagopa.selfcare.mscore.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.mscore.connector.rest.interceptor.OneMailAuthInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, OneMailAuthInterceptor.class})
public class OneMailRestClientConfig {
}
