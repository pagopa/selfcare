package it.pagopa.selfcare.commons.tenant;

public class UnknownStorageException extends IllegalArgumentException {

    public UnknownStorageException(String tenantId, String logicalKey) {
        super("Unknown storage '" + logicalKey + "' for tenant " + tenantId);
    }
}
