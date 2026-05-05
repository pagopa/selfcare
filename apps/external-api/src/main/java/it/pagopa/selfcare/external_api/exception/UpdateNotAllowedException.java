package it.pagopa.selfcare.external_api.exception;

public class UpdateNotAllowedException extends RuntimeException {

    public UpdateNotAllowedException(String message) {
        super(message);
    }

}
