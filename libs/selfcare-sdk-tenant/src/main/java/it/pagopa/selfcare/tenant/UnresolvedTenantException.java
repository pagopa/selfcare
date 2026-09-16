package it.pagopa.selfcare.tenant;

public class UnresolvedTenantException extends RuntimeException {

    public UnresolvedTenantException() {
        super("Tenant context has not been initialized");
    }
}
