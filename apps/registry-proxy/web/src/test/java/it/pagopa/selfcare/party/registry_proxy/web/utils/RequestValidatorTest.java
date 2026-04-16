package it.pagopa.selfcare.party.registry_proxy.web.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.selfcare.party.registry_proxy.web.exception.ValidationFailedException;
import org.junit.jupiter.api.Test;

class RequestValidatorTest {
    /**
     * Method under test: {@link RequestValidator#validateTaxId(String)}
     */
    @Test
    void testValidateTaxId() {
        assertThrows(ValidationFailedException.class, () -> RequestValidator.validateTaxId("42"));
        assertDoesNotThrow(() -> RequestValidator.validateTaxId("DLLDGI53T30I324E"));
    }

    /**
     * Method under test: {@link RequestValidator#validateTaxIdAndVatNumber(String, String)}
     */
    @Test
    void validateTaxIdAndVatNumber() {
        assertThrows(ValidationFailedException.class, () -> RequestValidator.validateTaxIdAndVatNumber("42", "DLLDGI53T30I324E"));
        assertThrows(ValidationFailedException.class, () -> RequestValidator.validateTaxIdAndVatNumber("DLLDGI53T30I324E", "42"));
        assertDoesNotThrow(() -> RequestValidator.validateTaxIdAndVatNumber("DLLDGI53T30I324E","01501320442"));
    }
}

