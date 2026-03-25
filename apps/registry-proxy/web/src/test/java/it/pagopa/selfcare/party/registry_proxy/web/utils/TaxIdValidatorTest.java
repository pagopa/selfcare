package it.pagopa.selfcare.party.registry_proxy.web.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaxIdValidatorTest {

    @Test
    void validateSuccess() {
        assertTrue(TaxIdValidator.validate("DLLDGI53T30I324E"));
    }

    @Test
    void validateError() {
        assertFalse(TaxIdValidator.validate("42"));
    }

    @Test
    void validateSuccessOnlyTaxId() {
        assertTrue(TaxIdValidator.validate("DLLDGI53T30I324E",true));
    }

    @Test
    void validateSuccessIva() {
        assertTrue(TaxIdValidator.validate("01501320442"));
    }
}