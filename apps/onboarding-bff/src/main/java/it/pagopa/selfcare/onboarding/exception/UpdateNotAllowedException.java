package it.pagopa.selfcare.onboarding.exception;

public class UpdateNotAllowedException extends RuntimeException {

    public UpdateNotAllowedException(String message) {
        super(message);
    }

}
