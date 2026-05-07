package it.pagopa.selfcare.dashboard.exception;

public class InternalGatewayErrorException extends RuntimeException {
    public InternalGatewayErrorException() {
    }

    public InternalGatewayErrorException(String message) {
        super(message);
    }
}
