package it.pagopa.selfcare.external_api.client.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserRegistryAuthInterceptor implements RequestInterceptor {

    private final TenantRegistry registry;
    private final TenantContext tenantContext;

    public UserRegistryAuthInterceptor(TenantRegistry registry, TenantContext tenantContext) {
        this.registry = registry;
        this.tenantContext = tenantContext;
        registry.supportedTenantIds().forEach(registry::userRegistryApiKey);
    }

    @Override
    public void apply(RequestTemplate template) {
        String apiKey = registry.userRegistryApiKey(tenantContext.requiredTenantId());
        template.removeHeader("x-api-key");
        template.header("x-api-key", apiKey);
    }

}
