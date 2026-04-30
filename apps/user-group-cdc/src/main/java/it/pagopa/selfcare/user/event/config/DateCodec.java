package it.pagopa.selfcare.user.event.config;

import org.bson.BsonReader;
import org.bson.BsonTimestamp;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;

public class DateCodec implements Codec<Instant> {

    @Override
    public Instant decode(BsonReader reader, DecoderContext decoderContext) {
        BsonType currentType = reader.getCurrentBsonType();
        if (currentType.equals(BsonType.DATE_TIME)) {
            return Instant.ofEpochMilli(reader.readDateTime());
        } else if (currentType.equals(BsonType.TIMESTAMP)) {
            BsonTimestamp timestamp = reader.readTimestamp();
            return Instant.ofEpochSecond(timestamp.getTime(), timestamp.getInc());
        }
        return null;
    }


    @Override
    public void encode(BsonWriter bsonWriter, Instant aLong, EncoderContext encoderContext) {

    }

    @Override
    public Class<Instant> getEncoderClass() {
        return Instant.class;
    }
}