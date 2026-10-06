package it.pagopa.selfcare.tenant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StorageAuthenticationType {
    MANAGED_IDENTITY,
    CONNECTION_STRING;

    @JsonCreator
    public static StorageAuthenticationType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Storage authentication type is required");
        }
        return StorageAuthenticationType.valueOf(value.trim().toUpperCase());
    }
}
