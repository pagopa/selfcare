package it.pagopa.selfcare.institution.exception;

import lombok.Getter;

@Getter
public class GenericException extends RuntimeException {

    public GenericException(String message) {
        super(message);
    }

}
