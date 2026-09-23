package it.pagopa.selfcare.commons.tenant;

public class UnknownTenantException extends IllegalArgumentException {

    public UnknownTenantException(String tenantId) {
        super("Unknown tenant: " + tenantId);
    }
}
