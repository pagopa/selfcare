package it.pagopa.selfcare.commons.tenant;

public class UnresolvedTenantException extends IllegalStateException {

    public UnresolvedTenantException() {
        super("Tenant context is not available");
    }
}
