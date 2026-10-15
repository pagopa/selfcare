package it.pagopa.selfcare.onboarding.exception;

public class InstitutionTypeNotAllowedException extends InvalidRequestException {

    public static final String CODE = "002-1010";

    public InstitutionTypeNotAllowedException(String message) {
        super(message, CODE);
    }
}
