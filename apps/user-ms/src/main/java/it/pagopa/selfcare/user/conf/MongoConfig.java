package it.pagopa.selfcare.user.conf;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

@ApplicationScoped
public class MongoConfig implements CodecProvider {

    @Override
    public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
        return type == OffsetDateTime.class
                ? CodecRegistries.fromCodecs(new DateCodec()).get(type)
                : null;
    }

}
