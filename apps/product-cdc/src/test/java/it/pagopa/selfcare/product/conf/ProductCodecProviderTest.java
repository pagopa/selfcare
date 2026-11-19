package it.pagopa.selfcare.product.conf;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.product.model.Product;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductCodecProviderTest {

  @Inject ProductCodecProvider provider;

  @Test
  void decodesChangeEventsWithObjectIdsAndStringIds() {
    ObjectId objectId = new ObjectId();
    assertEquals(objectId.toHexString(), decodeEvent(new BsonObjectId(objectId)).getId());
    assertEquals("legacy-string-id", decodeEvent(new BsonString("legacy-string-id")).getId());
  }

  @Test
  void stillEncodesProductIdAsString() {
    BsonDocument bson = new BsonDocument();
    registry()
        .get(Product.class)
        .encode(
            new BsonDocumentWriter(bson),
            Product.builder().id("existing-id").build(),
            EncoderContext.builder().build());
    assertEquals("existing-id", bson.getString("_id").getValue());
  }

  private Product decodeEvent(BsonValue id) {
    BsonDocument payload =
        new BsonDocument("operationType", new BsonString("insert"))
            .append("fullDocument", new BsonDocument("_id", id));
    return ChangeStreamDocument.createCodec(Product.class, registry())
        .decode(new BsonDocumentReader(payload), DecoderContext.builder().build())
        .getFullDocument();
  }

  private CodecRegistry registry() {
    return fromRegistries(
        fromProviders(provider),
        MongoClientSettings.getDefaultCodecRegistry(),
        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
  }
}
