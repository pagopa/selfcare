package it.pagopa.selfcare.mscore.connector.dao;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Exposes the tenant already validated by the web layer without depending on the web module.
 */
@Component
public class CurrentTenantProvider {

    /** Mirrors {@code TenantConstants#TENANT_REQUEST_ATTRIBUTE} in the {@code web} module. */
    static final String TENANT_REQUEST_ATTRIBUTE = "validatedTenantId";

    public Optional<String> currentTenantId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        Object tenant = attributes.getAttribute(TENANT_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return tenant instanceof Enum<?> tenantId ? Optional.of(tenantId.name()) : Optional.empty();
    }
}
