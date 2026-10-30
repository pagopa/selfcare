package it.pagopa.selfcare.azurestorage.error;

/** Thrown when the requested blob does not exist (Azure Storage responded 404). */
public class SelfcareAzureStorageNotFoundException extends SelfcareAzureStorageException {

    public SelfcareAzureStorageNotFoundException(String message, String code, Throwable cause) {
        super(message, code, cause);
    }
}
