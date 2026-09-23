package it.pagopa.selfcare.commons.tenant;

import java.util.function.Supplier;
import java.util.Optional;

public class TenantContext {

    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
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
