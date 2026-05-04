package it.pagopa.selfcare.user.conf;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateCodecTest {

    private final DateCodec dateCodec = new DateCodec();

    @Test
    void decode_withValidDateTime_shouldReturnOffsetDateTime() {
        LocalDateTime localDateTime = LocalDateTime.of(2021, 11, 1, 0, 0, 0);
        String epoch = String.valueOf(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
        BsonReader reader = new JsonReader("{\"date\": {\"$date\":" + epoch + "}}");
        reader.readStartDocument();
        reader.readName("date");
        Assertions.assertDoesNotThrow(() -> dateCodec.decode(reader, DecoderContext.builder().build()));
    }

    @Test
    void decode_withInvalidType_shouldReturnNull() {
        BsonReader reader = new JsonReader("{\"date\": \"invalid\"}");
        reader.readStartDocument();
        reader.readName("date");

        OffsetDateTime result = dateCodec.decode(reader, DecoderContext.builder().build());

        assertNull(result);
    }

    @Test
    void encode_withValidOffsetDateTime_shouldWriteDateTime() {
        OffsetDateTime dateTime = OffsetDateTime.of(2021, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        StringWriter stringWriter = new StringWriter();
        BsonWriter writer = new JsonWriter(stringWriter);

        Assertions.assertDoesNotThrow(() -> dateCodec.encode(writer, dateTime, EncoderContext.builder().build()));
    }

    @Test
    void getEncoderClass_shouldReturnOffsetDateTimeClass() {
        assertEquals(OffsetDateTime.class, dateCodec.getEncoderClass());
    }
}