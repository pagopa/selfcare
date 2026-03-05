package it.pagopa.selfcare.azurestorage.error;

public class SelfcareAzureStorageException extends RuntimeException {

    private final String code;

    public SelfcareAzureStorageException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
