package it.pagopa.selfcare.mscore.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;

@Slf4j
public class UserRegistryAuthInterceptor implements RequestInterceptor {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    public UserRegistryAuthInterceptor(TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.removeHeader("x-api-key");
        template.header("x-api-key",
                tenantRegistry.userRegistryApiKey(tenantContext.requiredTenantId()));
    }

}
