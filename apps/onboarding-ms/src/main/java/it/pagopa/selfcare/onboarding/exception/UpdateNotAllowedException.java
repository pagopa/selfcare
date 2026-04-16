package it.pagopa.selfcare.onboarding.exception;

import lombok.Getter;

@Getter
public class UpdateNotAllowedException extends RuntimeException {

    private final String code;

    public UpdateNotAllowedException(String message, String code) {
        super(message);
        this.code = code;
    }

    public UpdateNotAllowedException(String message) {
        super(message);
        this.code = "0000";
    }

}