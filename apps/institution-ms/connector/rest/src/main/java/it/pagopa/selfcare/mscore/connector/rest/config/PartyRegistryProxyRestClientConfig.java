package it.pagopa.selfcare.mscore.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor;
import it.pagopa.selfcare.commons.connector.rest.interceptor.PartyTraceIdInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, AuthorizationHeaderInterceptor.class, PartyTraceIdInterceptor.class})
public class PartyRegistryProxyRestClientConfig {
}
