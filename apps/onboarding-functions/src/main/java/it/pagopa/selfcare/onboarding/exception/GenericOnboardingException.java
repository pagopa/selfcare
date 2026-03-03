package it.pagopa.selfcare.onboarding.exception;

public class GenericOnboardingException extends RuntimeException {

    private final String code;

    public GenericOnboardingException(String message, String code) {
        super(message);
        this.code = code;
    }

    public GenericOnboardingException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
