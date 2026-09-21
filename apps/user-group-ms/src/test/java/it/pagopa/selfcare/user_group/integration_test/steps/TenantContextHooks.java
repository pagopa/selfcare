package it.pagopa.selfcare.user_group.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;

public class TenantContextHooks {

    @Autowired
    private TenantContext tenantContext;

    @Before(order = 0)
    public void setTenantContext() {
        tenantContext.setTenantId("AR");
    }

    @After(order = 0)
    public void clearTenantContext() {
        tenantContext.clear();
    }
}
