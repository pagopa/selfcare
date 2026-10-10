package it.pagopa.selfcare.document.config;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

/**
 * Forces the validation of the tenant registry at startup (fail-closed): TenantRegistry is a lazy
 * bean, so without this an invalid configuration would surface only on the first request.
 */
@ApplicationScoped
@Slf4j
public class TenantRegistryStartupValidator {

    void onStart(@Observes StartupEvent event, TenantRegistry tenantRegistry) {
        log.info("Tenant registry validated, supported tenants: {}", tenantRegistry.supportedTenantIds());
    }
}
