package it.pagopa.selfcare.dashboard.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BadGatewayException extends RuntimeException {

    public BadGatewayException(String message) {
        super(message);
    }
}
