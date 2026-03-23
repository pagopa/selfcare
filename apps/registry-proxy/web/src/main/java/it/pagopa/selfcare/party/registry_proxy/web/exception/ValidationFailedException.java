package it.pagopa.selfcare.party.registry_proxy.web.exception;

public class ValidationFailedException extends RuntimeException {
    public ValidationFailedException(String message) {
        super(message);
    }
}
