package it.pagopa.selfcare.onboarding.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException() {}
    public InvalidRequestException(String message) {
        super(message);
    }
}
