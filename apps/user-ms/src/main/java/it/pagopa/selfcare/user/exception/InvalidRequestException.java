package it.pagopa.selfcare.user.exception;

public class InvalidRequestException extends  RuntimeException{
    private final String code;

    public InvalidRequestException(String message, String code) {
        super(message);
        this.code = code;
    }

    public InvalidRequestException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
