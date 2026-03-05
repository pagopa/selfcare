package it.pagopa.selfcare.onboarding.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CustomOffsetDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public CustomOffsetDateTimeDeserializer() {
        super(OffsetDateTime.class);
    }

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String str = p.getText();
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        return dateTime.atOffset(ZoneOffset.UTC);
    }
}