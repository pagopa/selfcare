package it.pagopa.selfcare.onboarding.exception;

import lombok.Getter;

@Getter
public class IncrementRequiredException extends RuntimeException {

    private final String code;

    public IncrementRequiredException(String message, String code) {
        super(message);
        this.code = code;
    }

}
