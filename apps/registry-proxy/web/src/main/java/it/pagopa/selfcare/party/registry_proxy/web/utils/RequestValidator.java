package it.pagopa.selfcare.party.registry_proxy.web.utils;

import it.pagopa.selfcare.party.registry_proxy.web.exception.ValidationFailedException;

public class RequestValidator {
    private RequestValidator() {}
    public static void validateTaxId(String taxId) {
        if(!TaxIdValidator.validate(taxId)) {
            throw new ValidationFailedException("Tax id is invalid");
        }
    }

    public static void validateTaxIdAndVatNumber(String taxId, String vatNumber) {
        if(!TaxIdValidator.validate(taxId)) {
            throw new ValidationFailedException("Tax id is invalid");
        }

        if(!TaxIdValidator.validate(vatNumber)) {
            throw new ValidationFailedException("Vat number is invalid");
        }
    }
}
