package it.pagopa.selfcare.user.exception;

public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ConflictException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}