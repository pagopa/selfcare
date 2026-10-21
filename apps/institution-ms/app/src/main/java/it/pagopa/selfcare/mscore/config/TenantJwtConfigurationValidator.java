package it.pagopa.selfcare.mscore.config;

import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class TenantJwtConfigurationValidator {

    private final TenantRegistry tenantRegistry;

    public TenantJwtConfigurationValidator(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
    }

    @PostConstruct
    void validateTenantKeys() {
        tenantRegistry.supportedTenantIds().forEach(tenantId -> {
            if (tenantRegistry.resolve(tenantId).jwt() == null
                    || tenantRegistry.jwtPublicKey(tenantId).isEmpty()) {
                throw new IllegalStateException(
                        "Missing JWT verification key configuration for tenant " + tenantId);
            }
        });
    }
}
