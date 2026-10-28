package it.pagopa.selfcare.external_api.config;

import it.pagopa.selfcare.commons.web.config.SecurityConfig;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Slf4j
@Configuration
@EnableWebSecurity
@Import(SecurityConfig.class)
class ExternalAPISecurityConfig {

    ExternalAPISecurityConfig(TenantRegistry registry, Environment environment) {
        if (registry.supportedTenantIds().isEmpty()) {
            throw new IllegalStateException("External API requires a tenant registry");
        }
        if (registry.supportedTenantIds().size() > 1
                && !environment.getProperty("jwt.tenant-claim-required", Boolean.class, true)) {
            throw new IllegalStateException("Shared deployments require the JWT tenant claim");
        }
        for (String tenant : registry.supportedTenantIds()) {
            var jwt = registry.resolve(tenant).jwt();
            if (jwt == null || jwt.issuers().isEmpty() || jwt.audiences().isEmpty()) {
                throw new IllegalStateException(
                        "External API requires JWT keys, issuers and audiences for tenant " + tenant);
            }
        }
    }
}