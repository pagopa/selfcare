package it.pagopa.selfcare.azurestorage.error;

public enum SelfcareAzureStorageError {

    ERROR_DURING_UPLOAD_FILE("0000", "Error during upload file %s"),

    ERROR_DURING_DELETED_FILE("0000", "Error during deleted file %s"),
    ERROR_DURING_DOWNLOAD_FILE("0000", "Error during download file %s");

    private final String code;
    private final String detail;


    SelfcareAzureStorageError(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return detail;
    }
}
