package it.pagopa.selfcare.commons.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TenantContextTest {

    @Test
    void requiredTenantIdFailsWhenContextIsEmpty() {
        TenantContext context = new TenantContext();

        assertThrows(UnresolvedTenantException.class, context::requiredTenantId);
    }

    @Test
    void withTenantRestoresPreviousContext() {
        TenantContext context = new TenantContext();
        context.setTenantId("AR");

        String nested = context.withTenant("PNPG", context::requiredTenantId);

        assertEquals("PNPG", nested);
        assertEquals("AR", context.requiredTenantId());
    }
}
