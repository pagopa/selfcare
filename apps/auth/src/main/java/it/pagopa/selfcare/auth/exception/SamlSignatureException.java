package it.pagopa.selfcare.auth.exception;

import lombok.Getter;

@Getter
public class SamlSignatureException extends RuntimeException {
    private final String code;

    public SamlSignatureException(String message, String code) {
        super(message);
        this.code = code;
    }

    public SamlSignatureException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
