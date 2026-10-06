package it.pagopa.selfcare.party.registry_proxy.core.exception;

public class TooManyResourceFoundException extends RuntimeException {
    public TooManyResourceFoundException() {
    }

    public TooManyResourceFoundException(String message) {
        super(message);
    }
}
