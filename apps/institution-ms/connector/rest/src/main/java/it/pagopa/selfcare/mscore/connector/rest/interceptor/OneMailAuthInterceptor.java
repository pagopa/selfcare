package it.pagopa.selfcare.mscore.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;

public class OneMailAuthInterceptor implements RequestInterceptor {

    private static final String API_KEY_HEADER = "x-api-key";

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    public OneMailAuthInterceptor(TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @Override
    public void apply(RequestTemplate template) {
        TenantDefinition.OneMailCredentials credentials =
                tenantRegistry.oneMailCredentials(tenantContext.requiredTenantId());
        template.target(credentials.url());
        template.removeHeader(API_KEY_HEADER);
        template.header(API_KEY_HEADER, credentials.apiKey());
    }
}
