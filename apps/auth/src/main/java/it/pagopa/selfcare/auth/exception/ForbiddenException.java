package it.pagopa.selfcare.auth.exception;

public class ForbiddenException extends  RuntimeException{
    private final String code;

    public ForbiddenException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ForbiddenException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
