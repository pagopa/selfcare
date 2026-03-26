package it.pagopa.selfcare.onboarding.connector.exceptions;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException() {}
    public InvalidRequestException(String message) {
        super(message);
    }
}
