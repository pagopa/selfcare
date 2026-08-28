package it.pagopa.selfcare.external_api.client.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor;
import it.pagopa.selfcare.external_api.client.interceptor.TenantHeaderInterceptor;
import org.springframework.context.annotation.Import;

@Import({RestClientBaseConfig.class, AuthorizationHeaderInterceptor.class, TenantHeaderInterceptor.class})
public class MsPartyRegistryProxyRestClientConfig {
}
