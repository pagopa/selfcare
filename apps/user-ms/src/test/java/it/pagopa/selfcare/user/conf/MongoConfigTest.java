package it.pagopa.selfcare.user.conf;

import java.time.OffsetDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MongoConfigTest {
  @Test void codecProviderRoundTripsOffsetDateTimeAsBsonDate() {
    var provider = new MongoConfig();
    var codec = provider.get(OffsetDateTime.class, null);
    OffsetDateTime original = OffsetDateTime.parse("2025-01-02T03:04:05.123+02:00");
    BsonDocument document = new BsonDocument();
    var writer = new BsonDocumentWriter(document);
    writer.writeStartDocument();
    writer.writeName("date");
    codec.encode(writer, original, EncoderContext.builder().build());
    writer.writeEndDocument();
    var reader = new BsonDocumentReader(document);
    reader.readStartDocument();
    reader.readName();
    assertEquals(original.toInstant(), codec.decode(reader, DecoderContext.builder().build()).toInstant());
    assertNull(provider.get(String.class, null));
  }
}
