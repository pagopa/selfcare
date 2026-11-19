package it.pagopa.selfcare.product.conf;

import it.pagopa.selfcare.product.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.StringCodec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.PropertyModelBuilder;

@ApplicationScoped
public class ProductCodecProvider implements CodecProvider {

  private final PojoCodecProvider delegate =
      PojoCodecProvider.builder().register(productModel()).build();

  @Override
  public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
    return type == Product.class ? delegate.get(type, registry) : null;
  }

  private static ClassModel<Product> productModel() {
    ClassModelBuilder<Product> model = ClassModel.builder(Product.class);
    @SuppressWarnings("unchecked")
    PropertyModelBuilder<String> id = (PropertyModelBuilder<String>) model.getProperty("id");
    id.codec(
        new StringCodec() {
          @Override
          public String decode(BsonReader reader, DecoderContext context) {
            return reader.getCurrentBsonType() == BsonType.OBJECT_ID
                ? reader.readObjectId().toHexString()
                : super.decode(reader, context);
          }
        });
    return model.build();
  }
}
