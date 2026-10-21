package it.pagopa.selfcare.mscore.connector.dao;

public class TenantOwnershipConflictException extends RuntimeException {

    public TenantOwnershipConflictException() {
        super("Document ID is not available to the current tenant");
    }
}
