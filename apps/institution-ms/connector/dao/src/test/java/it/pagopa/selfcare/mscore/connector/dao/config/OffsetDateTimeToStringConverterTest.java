package it.pagopa.selfcare.mscore.connector.dao.config;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class OffsetDateTimeToStringConverterTest {

    @Test
    void testConvert() {
        String offsetDateTime = new OffsetDateTimeToStringConverter().convert(OffsetDateTime.now());
        Assertions.assertDoesNotThrow(() -> offsetDateTime);
    }
}

