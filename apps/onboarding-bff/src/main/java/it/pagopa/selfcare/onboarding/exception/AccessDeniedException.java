package it.pagopa.selfcare.onboarding.exception;

public class AccessDeniedException extends RuntimeException {

    public static final String MESSAGE = "Access Denied";

    public AccessDeniedException() {
        super(MESSAGE);
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
