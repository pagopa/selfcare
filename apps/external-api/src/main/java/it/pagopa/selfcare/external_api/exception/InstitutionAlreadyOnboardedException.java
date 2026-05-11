package it.pagopa.selfcare.external_api.exception;

public class InstitutionAlreadyOnboardedException extends RuntimeException {

    public InstitutionAlreadyOnboardedException(String message) {
        super(message);
    }

}
