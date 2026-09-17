package it.pagopa.selfcare.tenant;

public class UnknownStorageException extends RuntimeException {

    public UnknownStorageException(String tenantId, String logicalStorageKey) {
        super("Unknown storage '" + logicalStorageKey + "' for tenant " + tenantId);
    }
}
