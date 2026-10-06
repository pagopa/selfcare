package it.pagopa.selfcare.commons.tenant;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public class TenantContext {

    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant id is required");
        }
        currentTenant.set(tenantId.trim().toUpperCase(Locale.ROOT));
    }

    public String requiredTenantId() {
        String tenantId = currentTenant.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new UnresolvedTenantException();
        }
        return tenantId;
    }

    public Optional<String> tenantId() {
        return Optional.ofNullable(currentTenant.get()).filter(value -> !value.isBlank());
    }

    public void clear() {
        currentTenant.remove();
    }

    public <T> T withTenant(String tenantId, Supplier<T> action) {
        String previous = currentTenant.get();
        try {
            setTenantId(tenantId);
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                setTenantId(previous);
            }
        }
    }
}
