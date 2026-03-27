package it.pagopa.selfcare.document.exception;

import lombok.Getter;

/**
 * Exception thrown when PDF generation fails.
 */
@Getter
public class PdfBuilderException extends RuntimeException {

    private final String code;

    public PdfBuilderException(String message, String code) {
        super(message);
        this.code = code;
    }

    public PdfBuilderException(String message) {
        super(message);
        this.code = "0030";
    }

}
