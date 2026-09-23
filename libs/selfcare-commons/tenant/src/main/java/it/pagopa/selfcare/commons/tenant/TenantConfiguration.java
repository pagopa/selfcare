package it.pagopa.selfcare.commons.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TenantConfiguration {

    @Bean
    TenantContext tenantContext() {
        return new TenantContext();
    }

    @Bean
    TenantRegistry tenantRegistry(
            ObjectMapper objectMapper,
            Environment environment,
            @org.springframework.beans.factory.annotation.Value("${tenant.registry.json:{}}")
                    String registryJson,
            @org.springframework.beans.factory.annotation.Value("${tenant.supported-tenants:*}")
                    String supportedTenants,
            @org.springframework.beans.factory.annotation.Value("${tenant.storage.mandatory-keys:}")
                    String mandatoryStorageKeys) {
        return new TenantRegistry(
                objectMapper, environment, registryJson, supportedTenants, mandatoryStorageKeys);
    }
}
