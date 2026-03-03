package it.pagopa.selfcare.onboarding.exception;

public class FunctionOrchestratedException extends RuntimeException {

    public FunctionOrchestratedException(Exception e) {
        super(e);
    }
}
