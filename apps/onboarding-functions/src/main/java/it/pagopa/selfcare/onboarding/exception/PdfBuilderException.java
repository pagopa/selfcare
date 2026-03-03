package it.pagopa.selfcare.onboarding.exception;

import lombok.Getter;

@Getter
public class PdfBuilderException extends RuntimeException {

    private final String code;

    public PdfBuilderException(String message, String code) {
        super(message);
        this.code = code;
    }

    public PdfBuilderException(String message) {
        super(message);
        this.code = "0000";
    }

}
