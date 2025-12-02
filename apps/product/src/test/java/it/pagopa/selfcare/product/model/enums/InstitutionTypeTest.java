package it.pagopa.selfcare.product.model.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InstitutionTypeTest {

    @Test
    void fromTest_shouldReturnDefault_whenValueIsNull() {
        // given
        String value = null;

        // when
        InstitutionType result = InstitutionType.from(value);

        // then
        Assertions.assertEquals(InstitutionType.DEFAULT, result);
    }

    @Test
    void fromTest_shouldReturnDefault_whenValueIsBlank() {
        // given
        String value = "   ";

        // when
        InstitutionType result = InstitutionType.from(value);

        // then
        Assertions.assertEquals(InstitutionType.DEFAULT, result);
    }

    @Test
    void fromTest_shouldReturnDefault_whenValueIsUnknown() {
        // given
        String value = "UNKNOWN_TYPE";

        // when
        InstitutionType result = InstitutionType.from(value);

        // then
        Assertions.assertEquals(InstitutionType.DEFAULT, result);
    }

    @ParameterizedTest
    @CsvSource({
            "PA,PA",
            "pa,PA",
            "gSp,GSP"
    })
    void fromTest_shouldMatchValidTypesIgnoringCase(String input, InstitutionType expected) {
        // when
        InstitutionType result = InstitutionType.from(input);

        // then
        Assertions.assertEquals(expected, result);
    }
}
