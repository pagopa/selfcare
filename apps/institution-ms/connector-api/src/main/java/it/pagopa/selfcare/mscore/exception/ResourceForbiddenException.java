package it.pagopa.selfcare.mscore.exception;

public class ResourceForbiddenException extends RuntimeException{
    private final String code;

    public ResourceForbiddenException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
