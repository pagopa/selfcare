package it.pagopa.selfcare.auth.exception;

import lombok.Getter;

@Getter
public class ConflictException extends  RuntimeException {
    private final String code;

    public ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ConflictException(String message) {
        super(message);
        this.code = "409";
    }
}
