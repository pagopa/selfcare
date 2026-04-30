package it.pagopa.selfcare.user_group.exception;

public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ResourceAlreadyExistsException(String msg) {
        super(msg);
    }
}
