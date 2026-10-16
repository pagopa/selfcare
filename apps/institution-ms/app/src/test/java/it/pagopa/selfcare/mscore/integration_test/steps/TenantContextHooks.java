package it.pagopa.selfcare.mscore.integration_test.steps;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Value;

public class TenantContextHooks {

    private final TenantContext tenantContext;
    private final String defaultTenant;

    public TenantContextHooks(
            TenantContext tenantContext,
            @Value("${tenant.default:AR}") String defaultTenant) {
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    @Before(order = 0)
    public void setTenantContext() {
        tenantContext.setTenantId(defaultTenant);
    }

    @After(order = 0)
    public void clearTenantContext() {
        tenantContext.clear();
    }
}
