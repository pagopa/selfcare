package it.pagopa.selfcare.party.registry_proxy.connector.exception;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
