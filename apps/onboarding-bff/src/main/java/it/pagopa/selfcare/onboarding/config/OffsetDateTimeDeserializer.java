package it.pagopa.selfcare.onboarding.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                throw context.weirdStringException(
                        value,
                        OffsetDateTime.class,
                        "Unable to parse OffsetDateTime with or without timezone offset"
                );
            }
        }
    }
}
