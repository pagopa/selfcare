package it.pagopa.selfcare.party.registry_proxy.connector.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BadGatewayException extends RuntimeException {

    public BadGatewayException(String message) {
        super(message);
    }
}
