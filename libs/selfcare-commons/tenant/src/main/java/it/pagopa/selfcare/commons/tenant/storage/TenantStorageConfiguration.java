package it.pagopa.selfcare.commons.tenant.storage;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "tenant.storage.enabled", havingValue = "true")
public class TenantStorageConfiguration {

    @Bean
    TenantBlobClientProvider tenantBlobClientProvider(
            TenantRegistry tenantRegistry, TenantContext tenantContext) {
        return new TenantBlobClientProvider(tenantRegistry, tenantContext);
    }
}
