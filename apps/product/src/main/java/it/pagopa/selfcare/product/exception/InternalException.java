package it.pagopa.selfcare.product.exception;

public class InternalException extends  RuntimeException{
    private final String code;

    public InternalException(String message, String code) {
        super(message);
        this.code = code;
    }

    public InternalException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
