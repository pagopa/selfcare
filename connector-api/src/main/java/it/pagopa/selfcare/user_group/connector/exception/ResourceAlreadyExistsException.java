package it.pagopa.selfcare.user_group.connector.exception;

public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
