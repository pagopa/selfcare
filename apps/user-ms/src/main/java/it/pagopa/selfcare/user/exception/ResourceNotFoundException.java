package it.pagopa.selfcare.user.exception;

public class ResourceNotFoundException extends  RuntimeException{
    private final String code;

    public ResourceNotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
