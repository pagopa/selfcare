package it.pagopa.selfcare.mscore.exception;

public class InvalidRequestException extends  RuntimeException{
    private final String code;

    public InvalidRequestException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
