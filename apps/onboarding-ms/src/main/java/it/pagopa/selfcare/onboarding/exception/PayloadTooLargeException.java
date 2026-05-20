package it.pagopa.selfcare.onboarding.exception;

public class PayloadTooLargeException extends RuntimeException {

    private final String code;

    public PayloadTooLargeException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
