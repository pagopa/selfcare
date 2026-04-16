package it.pagopa.selfcare.onboarding.exception;

public class OnboardingNotAllowedException extends RuntimeException {

    private final String code;

    public OnboardingNotAllowedException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
