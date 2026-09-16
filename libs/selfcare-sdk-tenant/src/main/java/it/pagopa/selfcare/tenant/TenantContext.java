package it.pagopa.selfcare.tenant;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the tenant resolved for the current request or asynchronous operation.
 */
@RequestScoped
public class TenantContext {

    private String tenantId;

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String requiredTenantId() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new UnresolvedTenantException();
        }
        return tenantId;
    }

    public String getTenantId() {
        return requiredTenantId();
    }

    public boolean isInitialized() {
        return tenantId != null && !tenantId.isBlank();
    }

    public void clear() {
        tenantId = null;
    }
}
