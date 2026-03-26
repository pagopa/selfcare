package it.pagopa.selfcare.onboarding.connector.exceptions;

public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException() {
    }

    public ResourceConflictException(String message) {
        super(message);
    }
}
