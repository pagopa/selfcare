package it.pagopa.selfcare.dashboard.exception;

public class SupportException extends RuntimeException {
    public SupportException(String message) {
        super(message);
    }
    public SupportException(String message, Throwable t) {
        super(message, t);
    }
}
