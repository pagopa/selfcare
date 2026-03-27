package it.pagopa.selfcare.onboarding.connector.exceptions;

public class ManagerNotFoundException extends RuntimeException {
    public ManagerNotFoundException(String message) {
        super(message);
    }
}
