package it.pagopa.selfcare.external_api.exception;

public class OnboardingNotAllowedException extends RuntimeException {

    public OnboardingNotAllowedException(String message) {
        super(message);
    }

}
