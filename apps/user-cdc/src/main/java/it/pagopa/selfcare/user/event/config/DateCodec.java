package it.pagopa.selfcare.user.event.config;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class DateCodec implements Codec<OffsetDateTime> {

    @Override
    public OffsetDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        BsonType currentType = reader.getCurrentBsonType();
        if (currentType.equals(BsonType.DATE_TIME)) {
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(reader.readDateTime()), ZoneId.systemDefault());
        }
        return null;
    }


    @Override
    public void encode(BsonWriter bsonWriter, OffsetDateTime aLong, EncoderContext encoderContext) {
        bsonWriter.writeDateTime(aLong.toInstant().toEpochMilli());
    }

    @Override
    public Class<OffsetDateTime> getEncoderClass() {
        return OffsetDateTime.class;
    }
}