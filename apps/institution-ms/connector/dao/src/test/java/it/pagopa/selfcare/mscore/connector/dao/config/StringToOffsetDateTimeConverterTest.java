package it.pagopa.selfcare.mscore.connector.dao.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

class StringToOffsetDateTimeConverterTest {
    /**
     * Method under test: {@link StringToOffsetDateTimeConverter#convert(String)}
     */
    @Test
    void testConvert() {
        OffsetDateTime offsetDateTime = new StringToOffsetDateTimeConverter().convert("2011-12-03T10:15:30+01:00");
        Assertions.assertDoesNotThrow(() -> offsetDateTime);
    }
}

