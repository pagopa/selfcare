package it.pagopa.selfcare.auth.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {
    private final String code;

    public ForbiddenException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ForbiddenException(String message) {
        super(message);
        this.code = "403";
    }

}
